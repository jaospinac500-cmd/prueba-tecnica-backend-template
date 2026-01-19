# Respuestas - Prueba Técnica Backend Developer

## 1. Escenario de Concurrencia (Black Friday) 🏃‍♂️

### Problema
Es Black Friday y el sistema recibe 50 pedidos por segundo del iPhone 15 que solo tiene 10 unidades en stock. El resultado es un inventario negativo (-5 unidades).

### Pregunta
¿Qué mecanismo de base de datos o de Spring Boot utilizarías para asegurar que nunca se venda más stock del que existe, asumiendo múltiples instancias de la API corriendo en paralelo?

### Tu Respuesta
```
Para solucionar la condición de carrera (*Race Condition*) en un entorno de alta concurrencia distribuida,
implementaría un Bloqueo Pesimista (Pessimistic Locking) a nivel de base de datos.

Implementación Técnica:
Utilizaría la anotación `@Lock(LockModeType.PESSIMISTIC_WRITE)` de JPA en el repositorio de productos al momento de 
consultar el stock.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdWithLock(@Param("id") Long id);

```

---

## 2. Pregunta Trampa de Arquitectura 🎯

### Propuesta del Junior Developer
Configurar TODAS las relaciones JPA (`@OneToMany`, `@ManyToOne`) con `FetchType.EAGER` para:
- Traer toda la data en una sola consulta
- Evitar `LazyInitializationException`
- Mejorar el rendimiento

### Pregunta
¿Aceptarías este Pull Request? ¿Por qué sí o por qué no? ¿Qué impacto tendría con millones de registros?

### Tu Respuesta
```
- NO aceptaría el Pull Request. Porque cambiar globalmente a EAGER es una solución parche que introduce problemas
  graves de rendimiento y consumo de memoria, conocidos como el problema de la "carga en cascada" o N+1 queries ocultas.
  Y entendiendo el problema de "carga en cascada" el impacto seria que si la base de datos crece, EAGER obligará a 
  Hibernate a traer todos los datos relacionados en el momento de la consulta inicial. Un ejemplo que se podria dar para
  este problema es al consultar findAllOrders(), el sistema traería las Ordenes + todos sus Items + todos los Productos
  de esos items + cualquier relación del producto.
  
  Esto cargaría miles de objetos innecesarios en la memoria Heap de Java, provocando lentitud extrema en la red y muy
  probablemente un error fatal de OutOfMemoryError.
  
- El error LazyInitializationException se debe solucionar caso por caso, no globalmente, entonces podriamos hacer lo
  siguiente:
  
  1. JOIN FETCH: Usar consultas JPQL específicas (SELECT o FROM Order o JOIN FETCH o.items) solo cuando sepamos que
  vamos a necesitar los datos en esa transacción y esto con la ayuda de Spring Data JPA se podra manejar mejor.
  
  2. DTOs: Traer solo los datos necesarios mapeados a una clase plana (DTO) en lugar de entidades completas gestionadas.

Viendo la problematica que puede surgir y tambien siguiendo buenas practicas para no exigir y empezar a dimensionar en
recursos yo usaria LAZY por defecto para asegurar la escalabilidad del sistema de manera ordenada y coherente. El uso
EAGER solo si sé que esa vista específica de la aplicación necesitará mostrar todos los datos relacionados sí o sí.
  
```

---

## 3. Reflexiones Adicionales (Opcional) 💭

### Sobre el Refactoring Realizado
```
El objetivo principal fue eliminar el anti-patrón "God Method" en createOrder.

- Se aplicó el Principio de Responsabilidad Única (SRP) delegando validaciones, cálculos y persistencia a métodos
  privados semánticos.

- El código ahora se lee de manera declarativa:
  "Validar -> Inicializar -> Procesar Items -> Calcular Totales -> Guardar".

- Se aisló la lógica de reducción de stock para facilitar la futura implementación de los bloqueos de concurrencia
  mencionados en la pregunta 1.
  
```

### Patrones de Diseño Aplicados
```
En la refactorización no solo se limpió el código, sino que se reforzó la arquitectura utilizando los siguientes
patrones y paradigmas:

- Service Layer Pattern:
    Se mantuvo `OrderService` como la frontera transaccional. Esto porque encapsula la lógica de negocio
    (cálculo de totales, reglas de descuento) y orquesta el flujo, garantizando que el `Controller` se mantenga ligero
    y enfocado solo en HTTP.

- Repository Pattern (Existente):
    El uso de `OrderRepository` y `ProductRepository` permite abstraer la complejidad del acceso a datos. El servicio
    no necesita saber escribir SQL ni gestionar la conexión, solo pide objetos de dominio.

- Single Responsibility Principle (SRP):
    Al realizar la descomposición del método `createOrder` en `validateOrderRequest`, `processOrderItems` y
    `calculateFinalTotal` se logro eliminar el anti-patrón "God Method", lo cual, si cambia la forma de validar stock,
    solo se modifica un método privado sin riesgo de romper la lógica de descuentos.

- Functional Programming (Streams):
    En la implementación de la regla de descuento (`items.stream().map().distinct()`). Se pasó de un enfoque imperativo
    (bucles `for` anidados y contadores manuales) a uno declarativo, haciendo el código más conciso, legible y menos
    propenso a errores de estado mutable.

```

### Posibles Mejoras Futuras
```
Si tuviera más tiempo para iterar sobre este proyecto, implementaría las siguientes mejoras para robustecer la solución
productiva:

1.  Validación Declarativa (Bean Validation): Reemplazar las validaciones manuales (`if request.getName() == null`) por
    anotaciones estándar como `@NotNull`, `@Min(1)` y `@Email` directamente en los DTOs (`CreateOrderRequest`). Esto
    limpia el servicio de ruido defensivo.

2.  Mecanismo de Caching: Implementar caché de segundo nivel o Spring Cache sobre `productRepository.findById`. En un
    escenario como Black Friday, consultar la base de datos por el mismo producto ("iPhone 15") miles de veces por
    segundo es ineficiente; el caché reduciría drásticamente la latencia.

3.  Procesamiento Asíncrono (Event-Driven Architecture): Si el sistema escala, el envío de correos de confirmación o
    notificaciones no debería bloquear el hilo principal de la creación de la orden. Implementaría un bus de mensajes
    (RabbitMQ o Kafka) para publicar el evento `OrderCreated` y procesar notificaciones en segundo plano (side-effect).

4.  Auditoría de Datos (Spring Data Envers): Dado que manejamos transacciones financieras y stock, agregaría auditoría
    para rastrear quién modificó el stock y cuándo. Esto es vital para depurar inconsistencias en inventario.

5.  Observabilidad y Métricas: Integrar "Spring Boot Actuator" y "Micrometer" para monitorear métricas críticas en
    tiempo real, como "pedidos por segundo", "tiempo de respuesta del endpoint" y "errores de stock insuficiente".

```