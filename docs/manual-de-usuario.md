# Manual de Usuario — Inventory Industry

> **Versión:** 1.0.0  
> **Aplicación:** Inventory Industry — Gestión de inventario y producción de postes de luz de madera  
> **Plataforma:** Windows, macOS, Linux (aplicación de escritorio)  
> **Base de datos:** SQLite local en `~/.inventory-industry/inventory.db`

---

## Índice

1. [Introducción](#1-introducción)
2. [Primeros pasos](#2-primeros-pasos)
3. [Navegación y tema](#3-navegación-y-tema)
4. [Catálogo de productos](#4-catálogo-de-productos)
5. [Proveedores](#5-proveedores)
6. [Lotes de materia prima](#6-lotes-de-materia-prima)
7. [Traslados](#7-traslados)
8. [Insumos](#8-insumos)
9. [Recetas](#9-recetas)
10. [Transformaciones](#10-transformaciones)
11. [Fallos y salvamento](#11-fallos-y-salvamento)
12. [Clientes](#12-clientes)
13. [Ventas](#13-ventas)
14. [Contabilidad](#14-contabilidad)
15. [Historial](#15-historial)
16. [Panel de operaciones](#16-panel-de-operaciones)
17. [Solución de problemas](#17-solución-de-problemas)

---

## 1. Introducción

**Inventory Industry** es una aplicación de escritorio diseñada para gestionar el inventario, la producción, los costos y las ventas de **postes de luz de madera** en un flujo industrial de varias etapas.

### 1.1 Propósito del sistema

El sistema cubre el ciclo operativo y financiero completo de una planta que procesa postes de madera:

- Registrar materia prima y proveedores
- Trasladar lotes desde el predio del proveedor hasta la fábrica
- Avanzar lotes por etapas de producción (descortezado, tratamiento químico, terminado)
- Consumir insumos según recetas por etapa
- Gestionar fallos y stock de salvamento
- Calcular costos reales (adquisición, transporte, procesamiento)
- Registrar ventas con margen sugerido
- Consultar panel de control, historial y reportes contables

### 1.2 Etapas de producción

El núcleo del negocio es una cadena de **cuatro etapas** secuenciales:

| Etapa | Código | Descripción |
|-------|--------|-------------|
| **Crudo** | `CRUDO` | Poste en bruto recibido del proveedor |
| **Descortezado** | `DESCORTEZADO` | Corteza retirada y madera secada |
| **Tratado** | `TRATADO` | Tratamiento químico con preservante |
| **Terminado** | `TERMINADO` | Inspeccionado y listo para venta |

```
Crudo ──► Descortezado ──► Tratado ──► Terminado
  │            │             │
  └── Fallo ───┴── Fallo ───┘──► Salvamento
```

### 1.3 Ubicaciones de almacenamiento

Cada lote tiene una ubicación física:

| Ubicación | Descripción |
|-----------|-------------|
| **Proveedor** | Material aún en el predio del proveedor |
| **En tránsito** | Lote incluido en una corrida de transporte activa |
| **Fábrica** | Material recibido en planta, listo para procesar |

---

## 2. Primeros pasos

### 2.1 Ejecutar la aplicación

```bash
# Desarrollo
./gradlew run

# Empaquetado nativo
./gradlew packageDistributionForCurrentOS
```

### 2.2 Inicio por primera vez

Al iniciar la aplicación por primera vez:

1. Se crea automáticamente la base de datos SQLite en `~/.inventory-industry/inventory.db`
2. Se aplican las migraciones de esquema necesarias
3. Si las tablas están vacías, se cargan **datos semilla** predeterminados:
   - **28 insumos** precargados (preservantes, agua, energía, herrajes, etc.)
   - **17 líneas de receta** para las etapas Crudo, Descortezado y Tratado
   - **Lotes de stock de demostración** con cantidades y precios aleatorios

### 2.3 Gestión de usuarios

> **Nota:** Inventory Industry es una aplicación **monousuario** local. No incluye autenticación, inicio de sesión, roles ni permisos de usuario. Todos los datos residen en el equipo local y cualquier persona con acceso al equipo puede usar la aplicación. No existe una pantalla de login ni gestión de cuentas de usuario.

---

## 3. Navegación y tema

### 3.1 Barra lateral

La barra lateral izquierda proporciona acceso a las **11 pantallas** del sistema. Cada elemento tiene un icono y una etiqueta. La barra puede **colapsarse** para un espacio de trabajo más amplio.

| Icono | Pantalla | Descripción |
|-------|----------|-------------|
| 📊 | Panel | Resumen operativo con KPIs y gráficos |
| 📋 | Catálogo | Productos del catálogo maestro |
| 📦 | Por etapa | Inventario por etapa de producción |
| 🔬 | Insumos | Catálogo y stock de materiales |
| 📝 | Recetas | Consumo de insumos por etapa |
| 🚚 | Proveedores | Proveedores de materia prima |
| 🚛 | Traslados | Transporte desde el proveedor |
| 👥 | Clientes | Base de datos de clientes |
| 🛒 | Ventas | Registro de ventas |
| 💰 | Contabilidad | Movimientos contables |
| 📜 | Historial | Transformaciones registradas |

### 3.2 Paleta de comandos

Presione **Ctrl+K** para abrir la paleta de comandos, que permite:

- Ir a cualquier pantalla del menú principal
- Alternar tema claro / oscuro
- Colapsar o expandir la barra lateral

### 3.3 Tema claro / oscuro

La aplicación incluye un selector de tema claro y oscuro en la barra superior. El cambio es instantáneo y afecta a toda la interfaz. El tema se mantiene durante la sesión actual.

### 3.4 Barra superior

La barra superior muestra:

- **Migas de pan** (breadcrumbs): ruta de navegación actual (ej.: "Inventario > Panel")
- **Campo de búsqueda** (contextual según la pantalla activa)
- **Botón de tema** (alternar claro/oscuro)
- **Paleta de comandos** (Ctrl+K)

---

## 4. Catálogo de productos

### 4.1 Descripción

La pantalla **Catálogo** permite definir los tipos de poste que se manejan en el inventario. Cada producto del catálogo tiene un nombre, una línea de producto y una descripción opcional.

### 4.2 Crear un producto

1. Navegue a **Catálogo** en la barra lateral
2. Haga clic en el botón **+ Nuevo producto**
3. Complete los campos:
   - **Nombre** (obligatorio): ej. "Poste 10m", "Poste 8m Tratado"
   - **Línea** (obligatorio): ej. "Ligera", "Media", "Pesada"
   - **Descripción** (opcional): información adicional
4. Confirme con **Guardar**

### 4.3 Editar un producto

- Haga clic en el icono ✏️ en la fila del producto
- Modifique los campos necesarios
- Confirme con **Guardar**

### 4.4 Eliminar un producto

- Haga clic en el icono 🗑️ en la fila del producto
- Confirme la eliminación en el diálogo
- Los lotes existentes vinculados a este producto conservan la referencia (no se eliminan)

### 4.5 Búsqueda

Use el campo de búsqueda para filtrar productos por nombre, línea o descripción. Los resultados se actualizan en tiempo real.

---

## 5. Proveedores

### 5.1 Descripción

La pantalla **Proveedores** registra las empresas que suministran la materia prima (troncos de madera). Cada lote en etapa Crudo debe vincularse a un proveedor.

### 5.2 Crear un proveedor

1. Navegue a **Proveedores**
2. Haga clic en **+ Nuevo proveedor**
3. Complete los campos:
   - **Nombre** (obligatorio): ej. "Maderas ABC", "Pinos del Sur"
   - **Contacto** (opcional): teléfono, correo electrónico, persona de contacto
   - **Notas** (opcional): información adicional
4. Confirme con **Guardar**

### 5.3 Editar y eliminar

- **Editar**: icono ✏️ — modifique los datos y guarde
- **Eliminar**: icono 🗑️ — confirme la eliminación
  - Al eliminar un proveedor, los lotes existentes conservan el nombre del proveedor (no se eliminan en cascada)

---

## 6. Lotes de materia prima

### 6.1 Descripción

La pantalla **Por etapa** es el corazón operativo del sistema. Aquí se crean, gestionan y transforman los lotes de postes organizados por etapa de producción.

### 6.2 Crear un lote nuevo

1. Navegue a **Por etapa**
2. Asegúrese de estar en la pestaña **Crudo** (los lotes nuevos siempre ingresan como materia prima cruda)
3. Haga clic en el botón **+** (FAB) en la esquina inferior derecha
4. Complete los campos del formulario:

| Campo | Descripción |
|-------|-------------|
| **Nombre** | Nombre del lote (se sugiere automáticamente desde el catálogo) |
| **Línea** | Línea de producto |
| **Producto del catálogo** | Seleccione un producto del catálogo |
| **Proveedor** | Seleccione un proveedor registrado |
| **Cantidad** | Número de postes en el lote |
| **Costo de adquisición por poste** | Precio pagado al proveedor por cada poste |
| **Ubicación** | ¿Dónde se encuentra el lote? (Fábrica o Proveedor) |
| **Precio de venta estándar** | Precio de venta por poste cuando esté terminado |
| **Precio de venta fallado** | Precio de salvamento por poste si el lote falla |
| **Notas** | Información adicional |

5. Si la ubicación es **Proveedor**, puede agregar **líneas de costo de transporte** (camión, cargador, etc.) con su respectivo monto
6. Confirme con **Guardar**

### 6.3 Editar un lote

Haga clic en el icono ✏️ en la fila del lote. Puede modificar:

- Nombre y línea
- Cantidad
- Costos y precios
- Ubicación (si el lote está en Proveedor, puede cambiarlo a Fábrica directamente)
- Notas

### 6.4 Eliminar un lote

Haga clic en el icono 🗑️ y confirme la eliminación.

### 6.5 Búsqueda y filtros

- **Buscar**: por nombre de lote
- **Filtrar por línea**: desplegable con las líneas de producto disponibles
- **Exportar CSV**: copia el inventario filtrado al portapapeles en formato CSV

### 6.6 Paginación

Seleccione el número de filas por página (25, 50 o 100) y use los controles de paginación para navegar.

---

## 7. Traslados

### 7.1 Descripción

La pantalla **Traslados** gestiona el transporte de lotes desde el predio del proveedor hasta la fábrica. Incluye el registro de conductores y corridas de transporte con prorrateo automático de costos.

### 7.2 Conductores

#### Crear un conductor

1. En la pantalla **Traslados**, en el paso 1 del asistente, haga clic en **Agregar nuevo chofer**
2. Complete:
   - **Nombre** (obligatorio)
   - **Teléfono** (opcional)
   - **Notas** (opcional)
3. Confirme con **Guardar**

#### Editar / Eliminar conductor

- Use los iconos ✏️ y 🗑️ en las tarjetas de conductor
- No se puede eliminar un conductor que tenga corridas de transporte registradas

### 7.3 Iniciar un traslado

El asistente de 4 pasos guía todo el proceso:

**Paso 1 — Seleccionar chofer:**
- Seleccione un conductor de la lista (resaltado en violeta)
- O agregue uno nuevo si es necesario

**Paso 2 — Datos del transporte:**
- **Vehículo**: placa o identificación del vehículo
- **Costo de flete** (Bs): costo total del flete
- **Costo de grúa** (Bs): costo total de la grúa
- **Fecha de salida**: fecha y hora de partida
- **Llegada estimada**: fecha y hora estimada de llegada (opcional)
- **Notas**: información adicional

**Paso 3 — Seleccionar lotes:**
- Se muestran todos los lotes con ubicación **Proveedor**
- Seleccione uno o varios lotes para incluir en el traslado
- El contador muestra "X lote(s) en predio proveedor — Y seleccionado(s)"

**Paso 4 — Confirmar:**
- Revise el resumen completo: conductor, vehículo, fechas, costos y lotes
- Haga clic en **Iniciar Traslado**

Al iniciar, los lotes pasan a ubicación **En tránsito** y se crea la corrida con estado **En curso**.

### 7.4 Completar un traslado

Cuando el transporte llega a la fábrica:

1. En la sección **Historial de envíos**, localice la corrida en curso
2. Haga clic en **Registrar llegada**
3. Confirme la fecha y hora de llegada
4. El sistema **prorratea automáticamente** los costos de flete y grúa entre todos los lotes de la corrida, según su proporción de cantidad
5. Los lotes pasan a ubicación **Fábrica**

### 7.5 Cancelar un traslado

- Haga clic en **Cancelar envío** en la corrida en curso
- Los lotes vuelven a ubicación **Proveedor**
- La corrida se elimina

### 7.6 Indicadores

La parte superior muestra KPIs del módulo:

| Indicador | Descripción |
|-----------|-------------|
| Traslados (mes) | Número de traslados en el mes actual |
| En tránsito | Corridas activas actualmente |
| Completados | Corridas completadas este mes |
| Costo total | Suma de costos de flete y grúa del mes |
| Lotes trasladados | Total de lotes transportados |

---

## 8. Insumos

### 8.1 Descripción

La pantalla **Insumos** gestiona los materiales utilizados en el procesamiento de postes. Se divide en dos pestañas:

### 8.2 Catálogo de insumos (Pestaña 1)

Lista maestra de todos los materiales disponibles. La aplicación carga **28 insumos predeterminados** en la primera ejecución, organizados en categorías:

| Categoría | Ejemplos |
|-----------|----------|
| Materia prima | Tronco de pino, eucalipto |
| Preservantes | CCA, CCB, Creosota, ACQ |
| Agua | Agua para solución/limpieza |
| Autoclave/energía | Vapor, electricidad, combustible |
| Preparación | Sellador, pintura asfáltica, desinfectante |
| Herrajes | Placa metálica, grapa, perno, clavo |
| Acabados | Pintura protectora, barniz UV |
| Auxiliares | Solvente, kit EPP, neutralizante |

**Campos de un insumo:**
- **Nombre**: ej. "Creosota", "CCA"
- **Unidad**: L (litros), kg (kilogramos), m³ (metros cúbicos), kWh, ud (unidades)
- **Costo por unidad**: precio de referencia por unidad

#### Crear un insumo

1. En la pestaña **Catálogo de insumos**, haga clic en el FAB **+**
2. Complete nombre, unidad y costo por unidad
3. Confirme con **Guardar**

### 8.3 Inventario de insumos (Pestaña 2)

Gestiona los lotes de stock de cada insumo (compras reales).

**Campos de un lote de stock:**
- **Insumo**: seleccione del catálogo
- **Cantidad**: cantidad en la unidad del insumo
- **Precio de adquisición**: costo por unidad realmente pagado
- **Vencimiento**: fecha de expiración (opcional, formato aaaa-MM-dd)
- **Notas**: número de lote, factura, etc.

#### Crear un lote de stock

1. En la pestaña **Inventario (lotes)**, haga clic en el FAB **+**
2. Seleccione el insumo, ingrese cantidad, precio y opcionalmente vencimiento y notas
3. Confirme con **Guardar**

La pantalla muestra el **valor total estimado del inventario** basado en los lotes registrados.

> **Nota:** El sistema registra el consumo de insumos durante las transformaciones como costos de procesamiento, pero no descuenta automáticamente del stock físico. El ajuste del inventario físico es manual.

---

## 9. Recetas

### 9.1 Descripción

Las **Recetas** definen la cantidad esperada de cada insumo que se consume **por poste** al transformar de una etapa a la siguiente. Esto permite al sistema **sugerir automáticamente** las cantidades de insumos durante las transformaciones.

### 9.2 Crear una línea de receta

1. Navegue a **Recetas**
2. Seleccione la pestaña de la etapa de origen: **Crudo**, **Descort.** o **Tratado**
3. Haga clic en el FAB **+**
4. Complete:
   - **Insumo**: seleccione del catálogo de insumos
   - **Cantidad por poste**: cuánto se consume por cada poste procesado
   - **Orden de visualización**: número para ordenar las líneas
   - **Nota** (opcional): descripción o instrucción
5. Confirme con **Guardar**

### 9.3 Recetas predeterminadas

En la primera ejecución, el sistema carga recetas sugeridas:

**Crudo → Descortezado:**
| Insumo | Cant./poste |
|--------|-------------|
| Agua para solución/limpieza | 15 L |
| Desinfectante | 0.05 L |
| Vapor | 8 kg |
| Electricidad | 2 kWh |
| Combustible | 0.35 L |

**Descortezado → Tratado:**
| Insumo | Cant./poste |
|--------|-------------|
| Agua para solución/limpieza | 25 L |
| CCA | 0.45 kg |
| ACQ | 0.15 kg |
| Vapor | 12 kg |
| Combustible | 0.55 L |

**Tratado → Terminado:**
| Insumo | Cant./poste |
|--------|-------------|
| Sellador parafina | 0.18 kg |
| Capuchón | 1 ud |
| Pintura asfáltica | 0.1 L |
| Recubrimiento impermeabilizante | 0.05 L |
| Kit EPP | 0.02 ud |
| Barniz UV | 0.08 L |

---

## 10. Transformaciones

### 10.1 Descripción

Las transformaciones son el proceso de **avanzar lotes** de una etapa a la siguiente, consumiendo insumos y generando costos. Se realizan desde la pantalla **Por etapa**.

### 10.2 Modos de transformación

El sistema ofrece dos modos:

#### Modo A: Proceso en dos pasos (WIP)

Útil cuando el procesamiento toma tiempo y se quiere registrar el inicio antes de conocer los resultados.

**Iniciar proceso:**
1. En la pestaña de la etapa actual, seleccione los lotes a procesar
2. Haga clic en **Iniciar proceso → [siguiente etapa]**
3. Seleccione los lotes con casillas de verificación
4. Complete opcionalmente: fecha y notas
5. Confirme para crear el proceso **En curso**

El proceso aparece en la sección "Procesos en curso" de la pantalla.

**Completar proceso:**
1. Localice el proceso en curso
2. Haga clic en **Completar**
3. Ingrese:
   - **Postes exitosos**: cuántos pasaron correctamente a la siguiente etapa
   - **Postes fallados**: cuántos fallaron (opcional)
   - **Duración**: tiempo de procesamiento
   - **Consumo de insumos**: el sistema sugiere cantidades según la receta; puede ajustarlas manualmente
4. Confirme para completar la transformación

**Cancelar proceso:**
- Haga clic en **Cancelar** en el proceso en curso
- El proceso se descarta sin cambios en el inventario

#### Modo B: Un paso

Para transformaciones que se completan de inmediato.

1. Haga clic en **Registrar de una vez**
2. Seleccione los lotes fuente
3. Ingrese los resultados (éxitos y fallos)
4. Ajuste el consumo de insumos si es necesario
5. Confirme — el sistema crea los lotes resultantes y registra los costos de inmediato

### 10.3 Comportamiento del sistema

Durante una transformación, el sistema:

1. **Deduce** la cantidad de los lotes fuente (los elimina si se consumen por completo)
2. **Crea** uno o dos lotes nuevos:
   - Un lote **exitoso** en la siguiente etapa (con costo de adquisición mezclado ponderado)
   - Un lote **fallado** en la etapa actual (si hubo fallos)
3. **Registra** los costos de procesamiento (insumos consumidos × costo por unidad)
4. **Calcula** el costo de adquisición mezclado (promedio ponderado de los lotes fuente)

---

## 11. Fallos y salvamento

### 11.1 Marcar un lote como fallado

1. En la pantalla **Por etapa**, localice el lote en la etapa correspondiente
2. Haga clic en el icono ⚠️ (Marcar como fallado)
3. Seleccione:
   - **Etapa donde ocurrió el fallo** (Crudo, Descortezado o Tratado)
   - **Precio de salvamento**: precio de venta por poste fallado
   - **Notas** (opcional): descripción del fallo
4. Confirme

El lote muestra el estado **Fallado** (indicador rojo) y no puede seguir transformándose.

### 11.2 Revertir un fallo

- Haga clic en el icono ✅ (Limpiar fallo) en un lote fallado
- El lote vuelve a estado OK y puede continuar en el flujo de producción

### 11.3 Destino de lotes fallados

Los lotes fallados:
- Permanecen en el inventario con estado **Fallado**
- Se venden a **precio de salvamento** (distinto del precio estándar)
- Se muestran en la pantalla de Ventas como disponibles para vender
- Conservan el registro de en qué etapa ocurrió el fallo

---

## 12. Clientes

### 12.1 Descripción

La pantalla **Clientes** mantiene la base de datos de compradores de postes terminados o lotes fallados (salvamento).

### 12.2 Crear un cliente

1. Navegue a **Clientes**
2. Haga clic en **+ Nuevo cliente**
3. Complete:
   - **Nombre** (obligatorio): ej. "Constructora del Valle", "ELECSOL SRL"
   - **Contacto** (opcional): teléfono, correo, persona de contacto
   - **Notas** (opcional)
4. Confirme con **Guardar**

### 12.3 Editar y eliminar

- **Editar**: icono ✏️
- **Eliminar**: icono 🗑️
  - **Protegido**: no se puede eliminar un cliente que tenga ventas registradas. Debe eliminarlas primero o conservar el cliente.

También se pueden crear clientes **directamente desde el flujo de ventas**, sin necesidad de ir a la pantalla de Clientes.

---

## 13. Ventas

### 13.1 Descripción

La pantalla **Ventas** registra la salida de stock mediante la venta de postes, ya sean terminados OK o lotes fallados (salvamento). Incluye un asistente de 4 pasos con vista previa de costos y cálculo de margen.

### 13.2 Asistente de venta

**Paso 1 — Datos de la venta:**
- Seleccione un **cliente** de la lista o cree uno nuevo con **+ Nuevo cliente**
- Ingrese la **fecha y hora** de la venta
- Agregue **notas** si lo desea

**Paso 2 — Productos y cantidad:**
- Seleccione un **lote vendible** (Terminado OK o Fallado)
- Ingrese la **cantidad** a vender
- El sistema muestra la cantidad disponible y el precio de referencia

**Paso 3 — Costos y precios:**
- Ingrese un **porcentaje de margen** (ej.: 20 = 20% de margen)
- El sistema calcula y muestra:
  - **Costo de materia prima** por poste
  - **Transporte prorrateado** por poste
  - **Costo de procesamiento** por poste
  - **Costo unitario base** (material + transporte + procesamiento)
  - **Precio unitario sugerido** (base × (1 + margen))
  - **Total sugerido** (precio unitario × cantidad)
- Ingrese el **total a cobrar** manualmente o use **Usar precio sugerido** para auto-completar

**Paso 4 — Confirmar:**
- Revise el resumen completo: cliente, producto, cantidad, precios, margen y ganancia estimada
- Confirme para registrar la venta

### 13.3 Costos y precios

El desglose de costos en la vista previa:

| Concepto | Fórmula |
|----------|---------|
| Costo material por poste | `costoAdquisicionDelLote / cantidadDelLote` |
| Costo transporte por poste | `costosDeTransporteDelLote / cantidadDelLote` |
| Costo procesamiento por poste | `costosDeProcesamientoDelLote / cantidadDelLote` |
| Costo unitario base | `material + transporte + procesamiento` |
| Precio sugerido por poste | `costoBase × (1 + margen%)` |

### 13.4 Snapshots contables

Cada venta guarda una **fotografía inmutable** con:
- Nombre y línea del producto
- Etapa en que se encontraba
- Si era un lote fallado
- Nombre del proveedor original
- Costos de adquisición (material y transporte)
- Costo de procesamiento
- Margen aplicado
- Precio sugerido

Estos datos se conservan **aunque el lote se elimine después**, garantizando la integridad de los reportes históricos.

### 13.5 Ventas recientes

La sección inferior muestra las ventas más recientes en tarjetas, con:
- Nombre del cliente
- Fecha
- Producto y etapa
- Cantidad y total
- Botón **Ver detalle** para ver el desglose completo de costos

### 13.6 Reporte PDF

Haga clic en **Reporte PDF** para generar un reporte de ventas con selección de rango de fechas (diario, semanal, mensual, anual o personalizado).

---

## 14. Contabilidad

### 14.1 Descripción

La pantalla **Contabilidad** proporciona una visión financiera agregada del negocio.

### 14.2 Resumen de costos

Tres tarjetas muestran los costos acumulados:

| Tarjeta | Detalle |
|---------|---------|
| **Costos de procesamiento** | Total histórico, atribuido a stock abierto, imputado en ventas |
| **Costos de adquisición** | En inventario actual, imputado en ventas |
| **Costos de transporte** | Histórico total, en stock abierto, en ventas |

### 14.3 Agregación de ventas

Tres pestañas para visualizar las ventas agregadas:

| Pestaña | Parámetros | Muestra |
|---------|-------------|---------|
| **Diario** | Año + mes | Ventas por día del mes seleccionado |
| **Mensual** | Año | Ventas por mes (enero a diciembre) |
| **Anual** | (ninguno) | Ventas por año |

Cada fila muestra:
- **Período** (día, mes o año)
- **Número de ventas** realizadas
- **Postes vendidos** en total
- **Total facturado** (importe monetario)

---

## 15. Historial

### 15.1 Descripción

La pantalla **Historial** muestra el registro completo de todas las transformaciones realizadas, tanto completadas como en curso.

### 15.2 Visualización

Cada transformación se presenta en una tarjeta con:

- **Identificador** (#ID)
- **Transición**: ej. "Crudo → Descortezado"
- **Estado**: Completado o En curso
- **Fecha** de procesamiento
- **Métricas**: cantidad de entrada, exitosos, fallados, duración y costo de insumos
- **Lotes origen**: nombres de los lotes que se transformaron
- **Notas**: si se registraron durante la transformación

### 15.3 Paginación

Seleccione el número de registros por página: 5, 10, 25 o 50.

---

## 16. Panel de operaciones

### 16.1 Descripción

El **Panel** es la pantalla principal que ofrece un resumen operativo del negocio en un solo vistazo.

### 16.2 Indicadores (KPIs)

Cinco tarjetas muestran los indicadores clave:

| KPI | Descripción |
|-----|-------------|
| **En proceso** | Postes en etapas intermedias (Crudo + Descortezado + Tratado) |
| **Terminados OK** | Postes listos para venta estándar |
| **Fallados** | Stock de salvamento |
| **Lotes registrados** | Total de lotes en el sistema |
| **Valor inventario** | Costo de adquisición estimado del inventario |

### 16.3 Pestañas de análisis

**Resumen:**
- Gráfico de barras: producción por etapa
- Gráfico de dona: estado del inventario
- Actividad reciente: línea de tiempo con ventas, transformaciones y traslados
- Accesos rápidos a otras pantallas

**Producción:**
- Métricas de producción
- Gráfico de barras "Producción por etapa"
- Línea de tendencia de ventas del mes
- Tabla de producción por etapa

**Inventario:**
- Valor del inventario
- Costo de transformación
- Resumen y movimiento de stock por etapa

**Actividades:**
- Línea de tiempo filtrable por tipo (Ventas, Producción, Traslados)
- Búsqueda en actividades
- Exportar a PDF

### 16.4 Exportar PDF

Desde las pestañas **Resumen** y **Actividades** puede generar un reporte PDF del estado actual del inventario con la fecha del día.

---

## 17. Solución de problemas

### 17.1 La aplicación no inicia

**Causas posibles:**
- Java 21+ no está instalado
- La base de datos está corrupta

**Solución:**
```bash
java -version  # Verifique que Java 21+ esté disponible
rm -f ~/.inventory-industry/inventory.db  # Elimine la base de datos (se recreará automáticamente con datos semilla)
```

### 17.2 Error de base de datos

Si aparece un error de SQL o tabla no encontrada:
1. Cierre la aplicación
2. Elimine el archivo de base de datos: `rm -f ~/.inventory-industry/inventory.db`
3. Reinicie la aplicación. El sistema recreará la base de datos y cargará los datos semilla.

### 17.3 No aparecen datos en una pantalla

- **Insumos vacíos**: en la primera ejecución se cargan automáticamente. Si no aparecen, verifique que la base de datos se haya creado correctamente.
- **Recetas vacías**: se cargan junto con los insumos en la primera ejecución.
- **Sin lotes en Por etapa**: debe crear lotes manualmente desde el FAB **+**.
- **Sin clientes o proveedores**: debe registrarlos manualmente.

### 17.4 No puedo eliminar un cliente o conductor

- Los **clientes** con ventas registradas no se pueden eliminar. Elimine primero las ventas asociadas.
- Los **conductores** con corridas de transporte no se pueden eliminar.

### 17.5 El precio sugerido no aparece

Asegúrese de que el lote tenga configurado:
- Costo de adquisición por poste
- Los costos de transporte (si aplica)
- Los costos de procesamiento registrados

### 17.6 La aplicación es monousuario

Inventory Industry no tiene soporte multiusuario. Si necesita que varias personas usen el sistema, deben compartir el mismo equipo o copiar el archivo de base de datos (`~/.inventory-industry/inventory.db`) entre equipos.

---

© 2026 Inventory Industry. Aplicación de escritorio para gestión de producción de postes de madera.
