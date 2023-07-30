package com.lfy.context;

import com.lfy.annotation.*;
import com.lfy.exception.*;
import com.lfy.io.PropertyResolver;
import com.lfy.io.ResourceResolver;
import com.lfy.util.ClassUtils;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author:feiyang
 * @Date:7/29/2023 6:32 PM
 */
public class AnnotationConfigApplicationContext {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final PropertyResolver propertyResolver;

    protected final Map<String, BeanDefinition> beans;

    protected final Set<String> creatingBeanNames;

    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) throws IOException, URISyntaxException {
        this.propertyResolver = propertyResolver;
        //扫描获取所有Bean的Class类型
        Set<String> beanClassNames = scanForClassNames(configClass);

        //创建Bean的定义
        this.beans = createBeanDefinitions(beanClassNames);

        //创建BeanName检测循环依赖
        this.creatingBeanNames = new HashSet<>();

        //创建@Configuration类型的Bean
        this.beans.values().stream()
                //过滤出@Configuration：
                .filter(this::isConfigurationDefinition).sorted().map(def -> {
                    //创建Bean实例
                    createBeanAsEarlySingleton(def);
                    return def.getName();
                }).collect(Collectors.toList());

        // 创建BeanPostProcessor类型的Bean:
        List<BeanPostProcessor> processors = this.beans.values().stream()
                // 过滤出BeanPostProcessor:
                .filter(this::isBeanPostProcessorDefinition)
                // 排序:
                .sorted()
                // instantiate and collect:
                .map(def -> {
                    return (BeanPostProcessor) createBeanAsEarlySingleton(def);
                }).collect(Collectors.toList());
        this.beanPostProcessors.addAll(processors);

        //创建其他普通Bean：
        createNormalBeans();

    }

    boolean isConfigurationDefinition(BeanDefinition def) {
        return ClassUtils.findAnnotation(def.getBeanClass(), Configuration.class) != null;
    }

    boolean isBeanPostProcessorDefinition(BeanDefinition def) {
        return BeanPostProcessor.class.isAssignableFrom(def.getBeanClass());
    }

    /**
     * 创建普通的Bean
     */
    void createNormalBeans() {
        List<BeanDefinition> defs = this.beans.values().stream()
                //过滤出instance==null的BeanDefinition:
                .filter(def -> def.getInstance() == null)
                .sorted().collect(Collectors.toList());
        //依次创建Bean实例
        defs.forEach(def -> {
            //如果Bean未被创建（可能在其他Bean的构造方法注入前被创建）：
            if (def.getInstance() == null) {
                //创建Bean:
                createBeanAsEarlySingleton(def);
            }
        });
    }

    /**
     * 创建一个Bean，然后使用BeanPostProcessor处理，但不进行字段和方法级别的注入。如果创建的Bean不是Configuration或BeanPostProcessor，则在构造方法中注入的依赖Bean会自动创建。
     */
    Object createBeanAsEarlySingleton(BeanDefinition def) {
        //检测循环依赖
        if (!this.creatingBeanNames.add(def.getName())) {
            // 检测到重复创建Bean导致的循环依赖:
            throw new UnsatisfiedDependencyException(String.format("Circular dependency detected when create bean '%s'", def.getName()));
        }
        //创建方式：构造方法或工厂方法：
        Executable createFn = null;
        if (def.getFactoryName() == null) {
            //by constructor
            createFn = def.getConstructor();
        } else {
            //by factory method
            createFn = def.getFactoryMethod();
        }

        //创建参数：
        final Parameter[] parameters = createFn.getParameters();
        final Annotation[][] parametersAnnos = createFn.getParameterAnnotations();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            //从参数获取@Value和@Autowired:
            final Parameter param = parameters[i];
            final Annotation[] paramAnnos = parametersAnnos[i];
            final Value value = ClassUtils.getAnnotation(paramAnnos, Value.class);
            final Autowired autowired = ClassUtils.getAnnotation(paramAnnos, Autowired.class);
            //@Configuration类型的Bean是工厂，不允许使用@Autowired创建：
            final boolean isConfiguration = isConfigurationDefinition(def);
            if (isConfiguration && autowired != null) {
                throw new BeanDefinitionException(
                        String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.", def.getName(), def.getBeanClass().getName())
                );
            }
            //BeanPostProcessor不能依赖其他Bean，不允许使用@Autowired创建：
            final boolean isBeanPostProcessor = isBeanPostProcessorDefinition(def);
            if (isBeanPostProcessor && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify @Autowired when create BeanPostProcessor '%s': %s.", def.getName(), def.getBeanClass().getName())
                );
            }
            // 参数需要@Value或@Autowired两者之一:
            if (value != null && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify both @Autowired and @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }
            if (value == null && autowired == null) {
                throw new BeanCreationException(
                        String.format("Must specify @Autowired or @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }
            //参数类型
            final Class<?> type = param.getType();
            if (value != null) {
                //参数是@Value:
                args[i] = this.propertyResolver.getRequiredProperty(value.value(), type);
            } else {
                //参数是@Autowired:
                String name = autowired.name();
                boolean required = autowired.value();
                //依赖的BeanDefinition:
                BeanDefinition dependsOnDef = name.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(name, type);
                //检测required == true?
                if (required && dependsOnDef == null) {
                    throw new BeanCreationException(String.format("Missing autowired bean with type '%s' when create bean '%s': %s.", type.getName(),
                            def.getName(), def.getBeanClass().getName())
                    );
                }
                if (dependsOnDef != null) {
                    //获取依赖Bean
                    Object autowiredBeanInstance = dependsOnDef.getInstance();
                    if (autowiredBeanInstance == null && !isConfiguration && !isBeanPostProcessor) {
                        //当前依赖Bean尚未初始化，递归调用初始化该依赖Bean
                        autowiredBeanInstance = createBeanAsEarlySingleton(dependsOnDef);
                    }
                    args[i] = autowiredBeanInstance;
                } else {
                    args[i] = null;
                }
            }
        }
        //创建Bean实例
        Object instance = null;
        if (def.getFactoryName() == null) {
            //用构造方法创建
            try {
                instance = def.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        } else {
            //用@Bean方法创建：
            Object configInstance = getBean(def.getFactoryName());
            try {
                instance = def.getFactoryMethod().invoke(configInstance,args);
            }catch (Exception e){
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        }
        def.setInstance(instance);

        //调用BeanPostProcessor处理Bean:
        for (BeanPostProcessor processor : beanPostProcessors){
            Object processed = processor.postProcessBeforeInitialization(def.getInstance(), def.getName());
            if (processed == null){
                throw new BeanCreationException(String.format("PostBeanProcessor returns null when process bean '%s' by %s", def.getName(), processor));
            }
            //如果一个BeanPostProcessor处理Bean：
            if (def.getInstance() != processed){
                logger.atDebug().log("Bean '{}' was replaced by post processor {}.", def.getName(), processor.getClass().getName());
                def.setInstance(processed);
            }
        }
        return def.getInstance();
    }

    //通过Name查找Bean，不存在时抛出NoSuchBeanDefinitionExcetion
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition def = this.beans.get(name);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) def.getRequiredInstance();
    }


    protected Set<String> scanForClassNames(Class<?> configClass) throws IOException, URISyntaxException {
        //获取@ComponentScan注解
        ComponentScan scan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        // 获取注解配置的package名字,未配置则默认当前类所在包:
        String[] scanPackages = scan == null || scan.value().length == 0 ? new String[]{configClass.getPackage().getName()} : scan.value();

        Set<String> classNameSet = new HashSet<>();
        //依次扫描所有包：
        for (String pkg : scanPackages) {
            logger.atDebug().log("scan package: {}", pkg);
            //扫描一个包：
            var rr = new ResourceResolver(pkg);
            List<String> classList = rr.scan(res -> {
                //遇到以.class结尾的文件，就将其转换为Class全名：
                String name = res.name();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                return null;
            });
            //扫描结果添加到Set
            classNameSet.addAll(classList);
        }

        // 继续查找@Import(Xyz.class)导入的Class配置:
        Import importConfig = configClass.getAnnotation(Import.class);
        if (importConfig != null) {
            for (Class<?> importConfigClass : importConfig.value()) {
                String importClassName = importConfigClass.getName();
                classNameSet.add(importClassName);
            }
        }
        return classNameSet;
    }

    public Map<String, BeanDefinition> createBeanDefinitions(Set<String> classNameSet) {
        Map<String, BeanDefinition> defs = new HashMap<>();
        // 获取Class:
        for (String className : classNameSet) {
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }
            //是否标注@Component?
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            if (component != null) {
                //获取Bean的名称
                String beanName = ClassUtils.getBeanName(clazz);
                var def = new BeanDefinition(
                        beanName, clazz, getSuitableConstructor(clazz),
                        getOrder(clazz), clazz.isAnnotationPresent(Primary.class),
                        // init/destory方法名称
                        null, null,
                        //查找@PostConstruct方法：
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        //查找@PreDestroy方法：
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));
                addBeanDefinitions(defs, def);
                logger.atDebug().log("define bean: {}", def);

                Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
                if (configuration != null) {
                    scanFactoryMethods(beanName, clazz, defs);
                }
            }
        }
        return defs;
    }

    void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
        for (Method method : clazz.getDeclaredMethods()) {
            Bean bean = method.getAnnotation(Bean.class);
            if (bean != null) {
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
                }
                if (Modifier.isFinal(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be final.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be private.");
                }
                Class<?> beanClass = method.getReturnType();
                if (beanClass.isPrimitive()) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return primitive type.");
                }
                if (beanClass == void.class || beanClass == void.class) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return void.");
                }
                var def = new BeanDefinition(
                        ClassUtils.getBeanName(method), beanClass,
                        factoryBeanName,
                        //创建Bean的工厂方法
                        method,
                        //@Order
                        getOrder(method),
                        // 是否存在@Primary标注?
                        method.isAnnotationPresent(Primary.class),
                        // init方法名称:
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        // destroy方法名称:
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        // @PostConstruct / @PreDestroy method:
                        null, null);
                logger.atDebug().log("define bean: {}", def);
            }
        }
    }

    void addBeanDefinitions(Map<String, BeanDefinition> defs, BeanDefinition def) {
        if (defs.put(def.getName(), def) != null) {
            throw new BeanDefinitionException("Duplicate bean name: " + def.getName());
        }
    }

    /**
     * 获取公共构造函数或非公共构造函数作为反馈
     */
    public Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] cons = clazz.getConstructors();
        if (cons.length == 0) {
            cons = clazz.getDeclaredConstructors();
            if (cons.length != 1) {
                throw new BeanDefinitionException("More than one constructor found in class" + clazz.getName() + ".");
            }
        }
        if (cons.length != 1) {
            throw new BeanDefinitionException("More than one public constructor found in class " + clazz.getName() + ".");
        }
        return cons[0];
    }

    public int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    // 根据Name查找BeanDefinition，如果Name不存在，返回null
    @Nullable
    public BeanDefinition findBeanDefinition(String name) {
        return this.beans.get(name);
    }

    /**
     * 根据Name和Type查找BeanDefinition，如果Name不存在，返回null，如果Name存在，但Type不匹配，抛出异常。
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition def = findBeanDefinition(name);
        if (def == null) {
            return null;
        }
        if (!requiredType.isAssignableFrom(def.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.", requiredType.getName(),
                    name, def.getBeanClass().getName()));
        }
        return def;
    }

    /**
     * 根据Type查找若干个BeanDefinition，返回0个或多个。
     */
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream()
                // 按类型过滤:
                .filter(def -> type.isAssignableFrom(def.getBeanClass()))
                // 排序:
                .sorted().collect(Collectors.toList());
    }

    // 根据Type查找某个BeanDefinition，如果不存在返回null，如果存在多个返回@Primary标注的一个:
    @Nullable
    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> defs = findBeanDefinitions(type);
        if ((defs.isEmpty())) {
            return null;
        }
        if (defs.size() == 1) {
            return defs.get(0);
        }
        // 多于一个时，查找@Primary:
        List<BeanDefinition> primaryDefs = defs.stream()
                .filter(def -> def.isPrimary()).collect(Collectors.toList());
        if (primaryDefs.size() == 1) {
            return primaryDefs.get(0);
        }
        if (primaryDefs.isEmpty()) {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified", type.getName()));
        } else {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but multiple @Primary specified", type.getName()));
        }
    }
}
