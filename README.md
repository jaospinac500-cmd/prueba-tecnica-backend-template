# Prueba Técnica Backend Developer - Java & Spring Boot

**Rol:** Backend Developer  
**Tiempo Límite:** 5 días calendario  
**Formato de Entrega:** Fork + Pull Request en GitHub  

## Introducción

El objetivo de esta prueba no es solo ver si "el código funciona", sino evaluar tu capacidad para escribir **código limpio (Clean Code)** y tu **criterio arquitectónico**.

En lugar de construir todo desde cero, trabajarás sobre una base de código existente que necesita mejoras.

## 🚀 Cómo empezar

1. **Fork este repositorio** a tu cuenta personal de GitHub
2. **Clona** tu fork localmente
3. **Ejecuta la aplicación** para familiarizarte con ella:
   ```bash
   ./gradlew bootRun
   ```
4. **Accede a H2 Console** en http://localhost:8080/h2-console para ver los datos
   - URL: `jdbc:h2:mem:testdb`
   - Usuario: `sa`
   - Contraseña: (vacía)

## 📋 Parte 1: Refactorización y Nuevas Funcionalidades

### Tarea 1: Refactorización (Limpieza) ⚡

**Problema:** El método `OrderService.createOrder()` viola principios SOLID. Actualmente hace validaciones, cálculos y persistencia todo junto.

**Objetivo:** Refactoriza este flujo. Separa la lógica de:
- ✅ Validación de datos de entrada
- ✅ Validación de stock
- ✅ Cálculo de precios
- ✅ Aplicación de descuentos
- ✅ Persistencia en base de datos

**Criterio:** El código resultante debe ser legible casi como lenguaje natural.

### Tarea 2: Implementar Regla de Negocio Compleja 📊

Una vez refactorizado, implementa la siguiente lógica de negocio sobre el flujo de creación de pedidos:

**Regla del Descuento "Variedad":**
- Si un pedido contiene **más de 3 tipos de productos diferentes**, aplica un **10% de descuento** al total.
- **Nota:** No confundir "cantidad de items" con "tipos de productos".

**Ejemplos:**
- ❌ 10 unidades de Manzanas = NO descuento (1 tipo de producto)
- ✅ 1 Manzana, 1 Pera, 1 Uva, 1 Sandía = SÍ descuento (4 tipos distintos)

### Tarea 3: Testing 🧪

Escribe **Tests Unitarios** (JUnit 5 + Mockito) que verifiquen **exclusivamente la lógica del descuento**.

**Casos de prueba requeridos:**
- ✅ Pedido con 3 o menos tipos → NO descuento
- ✅ Pedido con más de 3 tipos → SÍ descuento (10%)
- ✅ Múltiples unidades del mismo producto → Contar como 1 tipo

## 📋 Parte 2: Escenario y Pregunta Trampa

Responde a estas preguntas en un archivo llamado **RESPUESTAS.md**.

### 1. El Escenario (Concurrencia) 🏃‍♂️

Es el "Black Friday". Tu sistema recibe **50 pedidos por segundo** del mismo producto (iPhone 15) que tiene solo **10 unidades** en stock.

Al revisar la base de datos, descubres que el stock ha quedado en **-5** (inventario negativo).

**Pregunta:** ¿Qué mecanismo de base de datos o de Spring Boot utilizarías para asegurar que nunca se venda más stock del que existe, asumiendo que tienes **múltiples instancias** de tu API corriendo en paralelo?

### 2. La Pregunta Trampa (Arquitectura) 🎯

Para mejorar el rendimiento de la aplicación y evitar las famosas excepciones de `LazyInitializationException` en las vistas, un desarrollador Junior propone configurar **todas** las relaciones de tus entidades JPA (`@OneToMany`, `@ManyToOne`) con `FetchType.EAGER`.

**Argumento del Junior:** *"Así nos traemos toda la data necesaria en una sola consulta SQL al principio y nos olvidamos de problemas de sesión cerrada después"*.

**Pregunta:** ¿Aceptarías este Pull Request? ¿Por qué sí o por qué no? Explica qué impacto tendría esto si la base de datos crece a **millones de registros**.

## 📤 Instrucciones de Entrega

### 1. **Fork**
Realiza un Fork del repositorio de la plantilla a tu cuenta personal.

### 2. **Desarrollo**
- ✅ Implementa la refactorización y la nueva lógica
- ✅ Asegúrate de que los tests pasen
- ✅ Ejecuta: `./gradlew test` para verificar

### 3. **Documentación**
- ✅ Edita este README.md explicando brevemente tus **decisiones de diseño**
- ✅ Crea **RESPUESTAS.md** con las respuestas a las preguntas

### 4. **Pull Request**
- ✅ Abre un PR hacia el repositorio original
- ✅ En la descripción del PR, menciona qué **patrones de diseño** aplicaste (si alguno)

## ⚖️ Criterios de Evaluación

| Criterio | Peso | ¿Qué evaluamos? |
|----------|------|-----------------|
| **Calidad del Refactor** | 40% | ¿El código es más limpio que el original? ¿Es fácil de leer? |
| **Corrección Lógica** | 30% | El descuento debe funcionar exactamente como se describe |
| **Testing** | 20% | Tests unitarios bien estructurados y que cubran los casos |
| **Profundidad en Respuestas** | 10% | Identificación de problemas de concurrencia y rendimiento |

## 🔧 Comandos Útiles

```bash
# Ejecutar la aplicación
./gradlew bootRun

# Ejecutar tests
./gradlew test

# Limpiar y compilar
./gradlew clean build

# Ver H2 Console
# http://localhost:8080/h2-console
```

## 📊 API Endpoints

```http
# Obtener todos los productos
GET http://localhost:8080/api/products

# Crear un pedido
POST http://localhost:8080/api/orders
Content-Type: application/json

{
  "customerName": "Juan Pérez",
  "customerEmail": "juan@email.com",
  "items": [
    {"productId": 1, "quantity": 2},
    {"productId": 2, "quantity": 1},
    {"productId": 3, "quantity": 1},
    {"productId": 4, "quantity": 1}
  ]
}

# Obtener todos los pedidos
GET http://localhost:8080/api/orders
```

---

## 🎯 Para el Candidato

**Recuerda:** No se trata solo de hacer que funcione. Se evalúa:
- 📖 **Legibilidad** del código
- 🏗️ **Separación de responsabilidades**
- 🧪 **Calidad de los tests**
- 💭 **Pensamiento arquitectónico**
- ✅ **Calidad en los commits**

**¡Mucho éxito! 🚀**


# Solución Implementada

A continuación se resumen las decisiones de diseño y refactorización aplicadas en este repositorio para la resolución de esta prueba técnica.

## 1. Refactorización **(Clean Code)**
   
   Se refactorizó la clase **OrderService** eliminando el anti-patrón God Method para cumplir con el Principio de Responsabilidad Única (SRP). El flujo principal se descompuso en métodos privados semánticos:
   
   - ***validateOrderRequest()***: Centraliza validaciones de entrada.

   - ***processOrderItems()***: Maneja la iteración, búsqueda de productos y control de stock.

   - ***calculateFinalTotal()***: Encapsula la lógica financiera y aplicación de descuentos.

   El código ahora es legible de manera declarativa y facilita el mantenimiento aislado de cada responsabilidad.


## 2. Lógica de Negocio (Descuento por Variedad)

   Se implementó la regla de "más de 3 tipos de productos" utilizando Java Streams. La clave de la solución es el uso de **.map(Product::getId).distinct().count()** sobre los IDs de los productos. Esto garantiza la distinción correcta entre "cantidad de items" vs "tipos de productos", asegurando que **n** unidades del mismo producto no activen el descuento, pero 4 productos distintos sí.
   
   Entonces si un cliente compra 10 unidades del mismo producto, el sistema cuenta "1 tipo", pero si compra 4 productos distintos (aunque sea 1 unidad de cada uno), aplica el 10% de descuento.

## 3. Testing

   Se desarrollaron pruebas unitarias aisladas con JUnit 5 y Mockito en **OrderServiceTest.java**. Se cubrieron los escenarios críticos exigidos:

   - ***Caso Base:*** Pedido con 3 tipos (Sin descuento).

   - ***Caso Éxito:*** Pedido con 4 tipos (Con descuento del 10%).

   - ***Caso Borde:*** Pedido con múltiples unidades de un solo tipo (Sin descuento).

## 4. Patrones de Diseño Aplicados

   - ***Service Layer:*** Encapsulamiento de la lógica de negocio.

   - ***Separation of Concerns (SoC):*** División estricta entre validación, persistencia y cálculo.

   - ***Repository Pattern:*** Abstracción del acceso a datos (JPA).

**Nota: *Las respuestas teóricas sobre el escenario de concurrencia (Black Friday) y la pregunta de arquitectura (JPA FetchTypes) se encuentran detalladas en el archivo RESPUESTAS.md.***