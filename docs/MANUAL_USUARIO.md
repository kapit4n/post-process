<div align="center">

# 🪵 Manual de Usuario

## Postes Industriales ERP

### Sistema de Gestión Integral para la Industria de Postes de Madera Tratada

---

**Versión:** 1.0  
**Última Actualización:** Julio 2026

</div>

---

## Índice

1. [Introducción](#1-introducción)
2. [Primeros Pasos](#2-primeros-pasos)
3. [Panel de Control](#3-panel-de-control)
4. [Catálogo de Productos](#4-catálogo-de-productos)
5. [Inventario por Etapa](#5-inventario-por-etapa)
6. [Insumos y Materiales](#6-insumos-y-materiales)
7. [Recetas por Etapa](#7-recetas-por-etapa)
8. [Proveedores](#8-proveedores)
9. [Traslados](#9-traslados)
10. [Clientes](#10-clientes)
11. [Ventas](#11-ventas)
12. [Contabilidad](#12-contabilidad)
13. [Historial](#13-historial)
14. [Generación de Reportes PDF](#14-generación-de-reportes-pdf)
15. [Consejos y Buenas Prácticas](#15-consejos-y-buenas-prácticas)

---

## 1. Introducción

**Postes Industriales ERP** es un sistema diseñado para gestionar el proceso completo de fabricación y venta de postes de madera tratada. Le permite controlar:

- **Inventario** de postes en cada etapa de producción
- **Costos reales** por poste (materia prima + transporte + procesamiento)
- **Ventas** con cálculo automático de márgenes
- **Logística** de transporte desde proveedores
- **Reportes** financieros y de inventario

### Requisitos del Sistema

| Componente | Mínimo | Recomendado |
|------------|--------|-------------|
| Sistema Operativo | Windows 10, macOS 12, Linux | Windows 11, macOS 14 |
| RAM | 4 GB | 8 GB |
| Disco | 500 MB | 1 GB |
| Resolución | 1280 × 720 | 1920 × 1080 |

---

## 2. Primeros Pasos

Al iniciar la aplicación por primera vez:

1. Se crea automáticamente una base de datos en `~/.inventory-industry/inventory.db`
2. Se cargan datos demo con precios reales de Bolivia
3. El **Panel de Control** se muestra con KPIs y gráficos actualizados

### Navegación General

La aplicación tiene una **barra lateral izquierda** con iconos de navegación:

| Icono | Módulo | Atajo |
|-------|--------|-------|
| 📊 | Panel | — |
| 📁 | Catálogo | — |
| 📦 | Por Etapa | — |
| 🧪 | Insumos | — |
| 📋 | Recetas | — |
| 🚛 | Proveedores | — |
| 🧭 | Traslados | — |
| 👥 | Clientes | — |
| 🛒 | Ventas | — |
| 🏦 | Contabilidad | — |
| 📜 | Historial | — |

**Atajos de Teclado:**
- `Ctrl + K` — Paleta de comandos (búsqueda rápida)

---

## 3. Panel de Control

![Panel de Control](../img/panel.png)

El Panel es la pantalla principal que muestra un resumen operativo del negocio.

### KPIs Principales

| KPI | Descripción |
|-----|-------------|
| **Postes** | Total de postes en el sistema |
| **En Inventario** | Postes disponibles para venta |
| **Valor Inventario** | Valor estimado total del inventario |
| **Procesamiento** | Costo acumulado de procesamiento |
| **Ventas** | Monto total de ventas del mes |
| **Margen** | Porcentaje promedio de ganancia |

### Gráficos

- **Distribución por Etapa** — Cantidad de postes en cada etapa de producción
- **Costos Mensuales** — Evolución de costos de procesamiento
- **Costos de Producción** — Desglose por tipo de costo

### Actividad Reciente

La pestaña **Actividad** muestra las últimas acciones registradas:
- Transformaciones completadas
- Ventas realizadas
- Traslados iniciados o completados

---

## 4. Catálogo de Productos

![Catálogo](../img/catalogo.png)

El Catálogo mantiene el registro maestro de tipos de postes que la empresa fabrica.

### Crear un Producto

1. Haga clic en **"+ Nuevo Producto"**
2. Complete los campos:
   - **Nombre** — Ej: "Poste de Madera 8m"
   - **Línea** — Ej: "Distribución Primaria"
   - **Descripción** — Descripción detallada
3. Haga clic en **"Guardar"**

### Editar un Producto

1. Haga clic en el **icono de lápiz** (✏️) de la fila
2. Modifique los campos deseados
3. Hace clic en **"Guardar"**

### Eliminar un Producto

1. Haga clic en el **icono de papelera** (🗑️) de la fila
2. Confirme la eliminación en el diálogo

### Búsqueda

Use el campo de **buscar** en la parte superior para filtrar productos por nombre o línea.

---

## 5. Inventario por Etapa

![Inventario por Etapa](../img/por-etapa.png)

Este módulo muestra el inventario organizado por etapa de producción. Es la pantalla más utilizada del sistema.

### Etapas de Producción

| Etapa | Descripción |
|-------|-------------|
| **Crudo** | Troncos recién adquiridos del proveedor |
| **Descortezado** | Troncos despellejados y secados |
| **Tratado** | Postes preservados químicamente en autoclave |
| **Terminado** | Postes listos para entrega al cliente |

### Navegación por Pestañas

Haga clic en las pestañas superiores para cambiar entre etapas:
- **Crudo** → **Descortezado** → **Tratado** → **Terminado**

### Crear un Nuevo Lote

1. Seleccione la pestaña **"Crudo"**
2. Haga clic en **"+ Nuevo Lote"**
3. Complete la información:
   - **Nombre** — Identificador del lote
   - **Línea** — Línea de producto
   - **Cantidad** — Número de postes
   - **Proveedor** — Proveedor de origen
   - **Costo por poste** — Precio de adquisición
   - **Precio de venta estándar** — Precio para postes OK
   - **Precio de saldo** — Precio para postes fallados
   - **Ubicación** — Fábrica, En Proveedor, En Tránsito
4. Haga clic en **"Guardar"**

### Transformar Postes entre Etapas

1. Seleccione un lote en la etapa origen
2. Haga clic en **"Transformar"** (→)
3. Defina:
   - **Cantidad a transformar**
   - **Cantidad exitosa** y **cantidad fallada**
   - **Duración** del proceso (horas)
   - **Costos de procesamiento** (se calculan automáticamente de las recetas)
4. Confirme la transformación

> **Nota:** Los postes fallados se mueven automáticamente a la etapa anterior con precio de saldo.

### Marcar un Poste como Fallado

1. Haga clic en el **icono de advertencia** (⚠️) del lote
2. Defina el **precio de venta de saldo**
3. Confirme

### Editar un Lote

1. Haga clic en el **icono de lápiz** (✏️) del lote
2. Modifique los campos necesarios
3. Haga clic en **"Guardar"**

### Eliminar un Lote

1. Haga clic en el **icono de papelera** (🗑️) del lote
2. Confirme la eliminación

---

## 6. Insumos y Materiales

![Insumos](../img/insumos.png)

Gestione todos los materiales consumibles en el proceso de tratamiento de postes.

### Pestañas

| Pestaña | Contenido |
|---------|-----------|
| **Catálogo** | Lista de todos los insumos disponibles |
| **Inventario** | Lotes de compra con precios y cantidades |

### Crear un Insumo

1. Vaya a la pestaña **"Catálogo"**
2. Haga clic en **"+ Nuevo Insumo"**
3. Complete:
   - **Nombre** — Ej: "Preservante · Creosota"
   - **Unidad** — kg, L, unidad, kWh
   - **Costo por unidad** — Precio de referencia
4. Haga clic en **"Guardar"**

### Registrar una Compra de Insumo

1. Vaya a la pestaña **"Inventario"**
2. Haga clic en **"+ Nueva Partida"**
3. Complete:
   - **Insumo** — Seleccione del catálogo
   - **Cantidad** — Unidades compradas
   - **Precio por unidad** — Precio real pagado
   - **Fecha de vencimiento** — Opcional
   - **Notas** — Referencia de la compra
4. Haga clic en **"Guardar"**

### Valor del Inventario

El sistema calcula automáticamente el **valor estimado del inventario** sumando (cantidad × precio) de todas las partidas activas.

---

## 7. Recetas por Etapa

![Recetas](../img/recetas.png)

Defina cuánto de cada insumo se consume por poste en cada etapa de producción.

### Crear una Receta

1. Seleccione la **etapa de origen** (Crudo, Descortezado, o Tratado)
2. Haga clic en **"+ Nueva Receta"**
3. Complete:
   - **Insumo** — Seleccione del catálogo
   - **Cantidad por poste** — Unidades consumidas por poste
   - **Notas** — Descripción del uso
4. Haga clic en **"Guardar""

### Ejemplo de Receta

Para la etapa **Crudo → Descortezado**:

| Insumo | Cantidad/Poste | Descripción |
|--------|---------------|-------------|
| Agua | 15 L | Lavado y preparación |
| Desinfectante | 0.05 L | Tratamiento inicial |
| Vapor | 8 kg | Secado |
| Electricidad | 2 kWh | Energía |
| Combustible | 0.35 L | Caldera |

> **Nota:** Las recetas se usan automáticamente al crear transformaciones para calcular los costos de procesamiento.

---

## 8. Proveedores

![Proveedores](../img/proveedores.png)

Gestione la información de sus proveedores de materia prima.

### Crear un Proveedor

1. Haga clic en **"+ Nuevo Proveedor"**
2. Complete:
   - **Nombre** — Razón social
   - **Contacto** — Teléfono (+591 ...)
   - **Notas** — Ubicación, especialidad, etc.
3. Haga clic en **"Guardar"**

### Editar o Eliminar

- **Editar:** Haga clic en el icono de lápiz (✏️)
- **Eliminar:** Haga clic en el icono de papelera (🗑️) y confirme

---

## 9. Traslados

![Traslados](../img/traslados.png)

Registre y administre el transporte de troncos desde los predios de los proveedores hasta la fábrica.

### Crear un Traslado

1. Haga clic en **"+ Nuevo Traslado"**
2. Complete la información del traslado:
   - **Chofer** — Seleccione o cree uno nuevo
   - **Placa del vehículo** — Formato: BB-LPV-12
   - **Fecha de salida** — Fecha programada
   - **Fecha de llegada estimada**
   - **Costo de flete** — Monto del flete
   - **Costo de grúa** — Monto de la grúa
   - **Notas** — Referencia del traslado
3. Seleccione los **lotes** a trasladar
4. Confirme el traslado

### Estados del Traslado

| Estado | Descripción |
|--------|-------------|
| **En Progreso** | El traslado está en curso |
| **Completado** | Los lotes llegaron a la fábrica |

### Completar un Traslado

1. Haga clic en **"Completar"** en el traslado activo
2. Confirme la llegada

> **Nota:** Los costos de transporte se distribuyen proporcionalmente entre los lotes según su cantidad.

### Gestionar Choferes

Los choferes se gestionan desde la pantalla de Traslados:
1. Haga clic en **"Gestionar Choferes"**
2. Cree, edite o elimine choferes

---

## 10. Clientes

![Clientes](../img/clientes.png)

Mantenga un registro de sus clientes y contactos.

### Crear un Cliente

1. Haga clic en **"+ Nuevo Cliente"**
2. Complete:
   - **Nombre** — Razón social
   - **Contacto** — Teléfono
   - **Notas** — Tipo de cliente, frecuencia de compra
3. Haga clic en **"Guardar"**

### Editar o Eliminar

- **Editar:** Haga clic en el icono de lápiz (✏️)
- **Eliminar:** Haga clic en el icono de papelera (🗑️) y confirme

---

## 11. Ventas

![Ventas](../img/ventas.png)

Registre ventas con cálculo automático de costos y márgenes de ganancia.

### Proceso de Venta (Paso a Paso)

#### Paso 1: Seleccionar Cliente

1. Haga clic en **"+ Nueva Venta"**
2. Seleccione el cliente de la lista

#### Paso 2: Seleccionar Productos

1. Busque y seleccione los postes a vender
2. Defina la **cantidad** de cada lote
3. El sistema muestra el **precio de referencia** por poste

#### Paso 3: Definir Precio

1. Ingrese el **monto total** a cobrar
2. O haga clic en **"Usar precio sugerido"** para calcular automáticamente
3. El sistema muestra:
   - **Precio por unidad**
   - **Margen estimado** (% y monto)
   - **Desglose de costos:**
     - Adquisición (material)
     - Transporte
     - Procesamiento

#### Paso 4: Confirmar

1. Revise el resumen de la venta
2. Haga clic en **"Registrar Venta"**
3. La venta queda registrada en el historial

### Historial de Ventas

Las ventas anteriores se muestran en la parte inferior de la pantalla con:
- Fecha y cliente
- Cantidad y monto total
- Costos snapshot (guardados al momento de la venta)

### Eliminar una Venta

1. Haga clic en el **icono de papelera** (🗑️) de la venta
2. Confirme la eliminación
3. El inventario se repone automáticamente

---

## 12. Contabilidad

![Contabilidad](../img/contabilidad.png)

Consulte el resumen financiero de la operación.

### Métricas Disponibles

| Métrica | Descripción |
|---------|-------------|
| **Procesamiento Acumulado** | Costo total histórico de procesamiento |
| **Procesamiento en Stock** | Costo de procesamiento en lotes abiertos |
| **Adquisición en Inventario** | Costo de materia prima en stock |
| **Traslado Histórico** | Costos totales de transporte |
| **Traslado en Stock** | Costos de transporte en lotes abiertos |
| **Ventas Registradas** | Ingresos totales por ventas |

### Agregación por Período

- **Diario** — Resumen del día actual
- **Mensual** — Resumen del mes actual
- **Anual** — Resumen del año actual

### Exportar a PDF

1. Haga clic en **"Exportar PDF"**
2. Seleccione la ubicación del archivo
3. El reporte se genera automáticamente

---

## 13. Historial

![Historial](../img/historial.png)

Consulte el registro completo de todas las actividades del sistema.

### Tipos de Eventos

| Evento | Descripción |
|--------|-------------|
| **Transformación** | Movimiento entre etapas de producción |
| **Venta** | Registro de venta a cliente |
| **Traslado** | Transporte desde proveedor |

### Información por Evento

Cada registro incluye:
- **Fecha y hora** del evento
- **Tipo** de operación
- **Detalles** (etapa origen → destino, cantidades)
- **Costos** asociados
- **Notas** adicionales

### Paginación

Use los controles inferiores para navegar entre páginas del historial.

---

## 14. Generación de Reportes PDF

El sistema genera reportes profesionales en formato PDF.

### Tipos de Reportes

| Reporte | Contenido | Desde |
|---------|-----------|-------|
| **Inventario** | Resumen de postes por etapa | Panel |
| **Por Etapa** | Detalle completo por etapa | Por Etapa |
| **Ventas** | Historial de ventas por rango | Ventas |
| **Contabilidad** | Resumen financiero | Contabilidad |

### Generar un Reporte

1. Haga clic en el botón **"Exportar PDF"** o **"Generar PDF"**
2. Si el reporte requiere rango de fechas:
   - Seleccione el período (Hoy, Semana, Mes, Año, Todo, Personalizado)
   - Defina las fechas de inicio y fin
3. Seleccione la ubicación del archivo en el diálogo del sistema
4. El reporte se genera y guarda automáticamente

### Contenido del Reporte

- Encabezado con fecha y título
- Tablas formateadas con datos
- Totales y subtotales
- Pie de página

---

## 15. Consejos y Buenas Prácticas

### Flujo de Trabajo Recomendado

```
1. Registrar Proveedores
       ↓
2. Crear Lotes (Crudo)
       ↓
3. Registrar Traslados
       ↓
4. Transformar: Crudo → Descortezado
       ↓
5. Transformar: Descortezado → Tratado
       ↓
6. Transformar: Tratado → Terminado
       ↓
7. Registrar Ventas
       ↓
8. Revisar Contabilidad
```

### Consejos

1. **Mantenga el catálogo actualizado** — Defina todos los tipos de postes antes de crear lotes
2. **Use recetas** — Las recetas permiten cálculos automáticos de costos
3. **Registre todos los traslados** — Los costos de transporte afectan el precio final
4. **Revise el Panel diariamente** — Los KPIs muestran el estado del negocio
5. **Exporte reportes mensualmente** — Para análisis y auditoría
6. **No elimine ventasRegistradas** — Use la función de eliminar solo cuando sea necesario

### Errores Comunes

| Error | Solución |
|-------|---------|
| "No se pudo guardar" | Verifique que todos los campos obligatorios estén completos |
| "Stock insuficiente" | Verifique la cantidad disponible en el lote |
| "Precio inválido" | Use números positivos sin caracteres especiales |

---

## Información de Contacto

Para soporte técnico o consultas, comuníquese con el equipo de desarrollo.

---

<div align="center">

*Manual de Usuario — Postes Industriales ERP v1.0*

</div>
