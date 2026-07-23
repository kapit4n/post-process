# Proyecto General — Inventory Industry

**Estado**: Activo  
**Fecha de inicio**: Julio 2026  
**Última actualización**: 23 de julio de 2026

## Descripción

**Inventory Industry** es una aplicación de escritorio ERP construida con Kotlin y Jetpack Compose Desktop, diseñada para gestionar el inventario, la línea de producción, los costos y las ventas de un plant de procesamiento industrial de postes de luz de madera.

## Objetivos del Proyecto

1. **Gestionar el inventario completo** de postes en todas las etapas de producción
2. **Controlar la línea de transformación**: CRUDO → DESCORTEZADO → TRATADO → TERMINADO
3. **Registrar y rastrear costos** de adquisición, traslado y procesamiento
4. **Gestionar relaciones** con proveedores, clientes y choferes
5. **Controlar traslados** de materia prima desde predios de proveedores a planta
6. **Registrar ventas** con snapshots contables inmutables
7. **Generar reportes** financieros y de inventario (PDF)
8. **Visualizar KPIs** y métricas en un dashboard operativo

## Alcance

### Pantallas (11)

| # | Pantalla | Función |
|---|----------|---------|
| 1 | Panel | Dashboard con KPIs, gráficos, actividad reciente |
| 2 | Catálogo | CRUD de tipos de poste |
| 3 | Por etapa | Inventario por etapa de producción (pantalla central) |
| 4 | Insumos | Catálogo de insumos + partidas de stock |
| 5 | Recetas | Recetas de consumo por etapa |
| 6 | Proveedores | Gestión de proveedores de materia prima |
| 7 | Traslados | Traslados proveedor → planta (viajes, conductores) |
| 8 | Clientes | Gestión de clientes |
| 9 | Ventas | Registro de ventas con preview de costos |
| 10 | Contabilidad | Vista financiera (diario/mensual/anual) |
| 11 | Historial | Registro de transformaciones (auditoría) |

### Flujo Operativo Principal

```
Compra de troncos → Registro en Crudo → Traslado a planta
→ Descortezado y Secado → Tratamiento Químico → Acabado y Empaque
→ Venta al cliente (con control de costos completo)
```

## Equipo / Responsable

- Desarrollo completo: Equipo GI
- Stack: Kotlin 2.0 + Compose Desktop 1.7 + SQLite
