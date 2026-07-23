# Mejoras Pendientes — Inventory Industry

**Última actualización**: 23 de julio de 2026

## Prioridad Alta

- [x] **Tests unitarios**: 143 tests cubriendo CRUD, costos, ventas, transporte, edge cases, dominio
- [x] **Tests de UI**: 38 tests de Compose Desktop para navegación y pantallas individuales
- [x] **Manejo de errores**: ErrorCategory tipado, UserMessage, exportPdfWorkflow, try/catch en todas las pantallas CRUD
- [ ] **Validación de entrada**: Fortalecer validaciones en formularios de edición (Milestone 4)

## Prioridad Media

- [ ] **Búsqueda global**: Implementar Ctrl+K command palette con búsqueda real
- [ ] **Exportación CSV**: Exportar inventario y ventas a CSV
- [ ] **Backup automático**: Copia de seguridad automática de la base de datos
- [ ] **Multi-idioma**: Soporte para español e inglés
- [ ] **Mejoras de UX en «Por etapa»**: Drag-and-drop para seleccionar lotes
- [ ] **Historial de precios**: Tracking de cambios de precio de insumos
- [ ] **Alertas de stock bajo**: Notificación cuando insumos están por debajo de umbral

## Prioridad Baja

- [ ] **Reportes personalizados**: Configurar campos y filtros para reportes
- [ ] **Gráficos interactivos**: Tooltips y drill-down en gráficos del dashboard
- [ ] **Modo kiosk**: Pantalla completa sin sidebar para presentaciones
- [ ] **Soporte multi-usuario**: Autenticación y permisos (requiere arquitectura cliente-servidor)
- [ ] **API REST**: Exponer datos vía API para integración con otros sistemas
- [ ] **Sincronización cloud**: Backup y sincronización con almacenamiento en la nube

## Deuda Técnica

- [ ] **Refactorizar InventoryRepository**: El archivo tiene 2200+ líneas. Considerar dividir en repositorios más pequeños por dominio
- [ ] **Separar concerns en Seed.kt**: La lógica de seed de inventario es compleja; considerar módulos separados
- [ ] **Migraciones manuales**: Actualmente se usa auto-migración. Para producción considerar migraciones versionadas
- [ ] **Logging**: Agregar logging estructurado para debugging y auditoría

## Documentación Pendiente

- [ ] User stories detalladas por pantalla
- [ ] Guía de contribución para desarrolladores
- [ ] Changelog / Release notes
