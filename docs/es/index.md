# Occtet Curator – Guía de usuario

## 1. Introducción

### 1.1 ¿Qué es Occtet Curator?

Occtet Curator es una plataforma profesional diseñada para analizar, curar y evaluar SBOMs (Software Bill of Materials), así como para generar informes VEX.

Una SBOM describe todos los componentes de software que forman parte de un proyecto. Esta información es fundamental para:

- Análisis de seguridad (CVE y vulnerabilidades)
- Cumplimiento de licencias
- Análisis de derechos de autor
- Evaluación de riesgos en la cadena de suministro de software

Occtet Curator transforma una SBOM sin procesar en unidades estructuradas y verificables que pueden ser revisadas por personas.
El elemento central en este proceso es el **InventoryItem**.

### 1.2 Conceptos clave

| Término | Descripción |
| --- | --- |
| SBOM | Lista de todos los componentes de software de un proyecto (por ejemplo, SPDX). |
| InventoryItem | Representación estructurada de un componente de software, incluyendo archivos, licencias, derechos de autor y CVEs. |
| SoftwareComponent | El componente de software en sí (por ejemplo, OpenSSL 3.0.2). |
| Vulnerability / CVE | Identificador estándar que indica si un componente de software está afectado por una vulnerabilidad conocida. |
| VEX | Indica si una vulnerabilidad (CVE) es relevante para el proyecto. |
| AuditNote | Documentación utilizada para justificar y registrar decisiones. |

## 2. Flujo de trabajo: cómo trabajar con Occtet

Occtet Curator sigue un enfoque basado en proyectos. Una organización puede contener varios usuarios y proyectos.

El administrador es responsable de:

- Crear organizaciones
- Asignar usuarios a las organizaciones
- Definir la ruta del sistema donde se almacena el código fuente

El curador trabaja normalmente de la siguiente manera:

1. Crear un proyecto (Projects)
2. Importar una SBOM (Import → SPDX Import o mediante ORT Starter si hay una instancia ORT disponible)
3. Acceder a SBOM Management (SBOMmgmt), seleccionar el proyecto y comenzar la curación:
   - InventoryItems a la derecha
   - Árbol de archivos a la izquierda
4. Revisar en la vista Overview las vulnerabilidades, licencias y derechos de autor
5. Trabajar con los InventoryItems:
   - Contienen información detallada sobre los SoftwareComponents
   - Incluyen vulnerabilidades, licencias y derechos de autor
   - Agrupan archivos asociados o archivos independientes con características comunes (por ejemplo, la misma licencia)
   - Permiten añadir AuditNotes para documentar decisiones y evidencias
6. Crear y descargar informes VEX en el contexto de un InventoryItem
7. Descargar la SBOM curada desde la página del proyecto

El sistema está diseñado para convertir una SBOM sin procesar en un inventario de software verificado y listo para auditoría.

Las siguientes secciones describen las distintas áreas del sistema con más detalle.

## 3. Configuración (Admin)

### 3.1 Definir la ruta del sistema

La ruta del sistema determina dónde se almacena el código fuente de los proyectos.
Es necesaria para poder visualizar el contenido de los archivos en SBOM Management.
Sin esta ruta, no se pueden crear proyectos.

### 3.2 Autocompletado de texto (sugerencias)

Por defecto, existen algunas sugerencias de autocompletado.
Estas pueden utilizarse en AuditNotes o como nombre de InventoryItems.

Solo el administrador puede crear nuevas sugerencias.
Actualmente existen dos contextos específicos que deben utilizarse tal como están definidos.

### 3.3 Términos de búsqueda

Se pueden definir perfiles de términos de búsqueda para resaltar palabras clave en el contenido de los archivos.

Ejemplos comunes: itext, gpl, lgpl, stackoverflow.
Los términos deben introducirse como una lista separada por comas.

### 3.4 Api Key

El administrador puede generar una clave API que se puede utilizar para la integración con Vulnerable Code.
Esto es necesario para obtener datos sobre vulnerabilidades.
La clave se obtiene aquí, en Vulnerable Code: <https://public.vulnerablecode.io/account/request_api_key/>

## 4. Organización (Admin)

El administrador crea organizaciones y asigna usuarios a ellas.
También puede asignar proyectos a organizaciones.
Todos los usuarios, ya sean administradores o curadores,
deben estar asignados a una organización para poder utilizar todas las funciones.

- Si el administrador crea un proyecto, puede seleccionar la organización
- Si un curador crea un proyecto, se asigna automáticamente a su organización

## 5. Proyectos

### 5.1 Crear un proyecto

Los siguientes campos son obligatorios:

- Nombre
- Versión
- Responsable del producto

Campos opcionales:

- Correo electrónico de contacto
- Perfil de búsqueda

Los datos de los proyectos dentro de una organización pueden reutilizarse.
Las licencias y los derechos de autor no están ligados a una organización específica y pueden utilizarse globalmente.

## 6. Importar una SBOM

Puedes importar una SBOM (actualmente en formato SPDX) mediante la función de carga de archivos. Durante la importación, también puedes configurar el uso de flujos de trabajo basados en IA para:

- detección de derechos de autor
- clasificación de licencias

Tras la importación, Occtet genera automáticamente:

- SoftwareComponents
- InventoryItems
- Vulnerabilidades (CVEs)
- Archivos
- Licencias
- Derechos de autor

## 7. Iniciar un ORT Run

En la página de importación también encontrarás un botón para iniciar un ORT run. Para utilizar esta función, es necesario que haya una instancia de ORT (Docker) en ejecución e integrada en el sistema. Los datos de conexión pueden configurarse en el archivo `application.properties` o mediante un archivo `.env`.

Al configurar un ORT run, debes definir:

- el tipo de repositorio a analizar
- la URL del repositorio
- la versión

## 8. Panel de control

El panel de control actúa como un indicador de seguridad del proyecto. Muestra, entre otros:

- el número de CVEs por nivel de gravedad (Critical, High, Medium, Low)
- cuántos componentes de software presentan vulnerabilidades

Esto te permite evaluar rápidamente si un proyecto está listo para su lanzamiento o si representa un riesgo.

## 9. Gestión de SBOM

Este es tu espacio de trabajo principal:

- izquierda: árbol de archivos
- derecha: InventoryItems
- centro: Overview (vulnerabilidades, licencias, derechos de autor)

Idealmente, cada archivo debe asignarse a un único InventoryItem.

## 10. InventoryItems – Elemento central

Un InventoryItem representa un componente de software e incluye:

- SoftwareComponent (nombre, versión, URL de origen)
- archivos asociados
- vulnerabilidades (CVEs) (con posibilidad de crear entradas VEX)
- licencias
- derechos de autor
- AuditNotes para documentar decisiones

Los InventoryItems son la base de tu análisis y permiten dividir la SBOM en unidades manejables.

## 11. SoftwareComponents

Los componentes de software se crean automáticamente al leer la SBOM. Cada componente de software pertenece a un elemento de inventario.
También es posible crear componentes de software manualmente. Esto se puede hacer directamente en la gestión de la SBOM, en el elemento de inventario
o en la vista de componentes de software. El nombre y la versión son campos obligatorios. Si se crea el componente en la vista de componentes de software
, también hay que crear un elemento de inventario para él y asignarle el componente de software.
El InventoryItem es el vínculo con un proyecto. Sin el InventoryItem, el SoftwareComponent no puede asignarse a ningún proyecto.

## 12. Trabajo con archivos

### 12.1 Visor de código

Puedes:

- abrir archivos
- leerlos
- buscarlos
- asignarlos a un InventoryItem

Durante la revisión, puedes encontrar información relevante como:

- encabezados de licencia
- avisos de copyright
- referencias a código de terceros

Esta información debe documentarse en las AuditNotes para garantizar la trazabilidad de las decisiones.
También deben registrarse posibles restricciones de exportación u otras notas legales detectadas.
Los términos de búsqueda definidos en tu perfil se resaltan en el contenido.

### 12.2 Estado “Reviewed”

Un archivo se considera revisado cuando:

- está asignado correctamente
- su licencia ha sido verificada
- sus derechos de autor han sido revisados
- toda la información ha sido documentada

Una vez completado, se marca como **reviewed**.

## 13. AuditNotes

Las AuditNotes sirven como documentación y evidencia. Permiten registrar, por ejemplo:

- por qué una licencia es aceptable
- por qué una CVE no es relevante
- por qué un copyright puede ignorarse

Son esenciales para:

- auditorías (por ejemplo, ISO)
- transparencia ante clientes
- documentación legal

## 14. Vulnerabilidades y VEX

Occtet detecta automáticamente las CVEs asociadas a cada SoftwareComponent. Puedes evaluarlas mediante estados VEX:

- Affected
- Not Affected
- Under Investigation
- Fixed

Estas evaluaciones pueden exportarse en formato VEX (Vulnerability Exploitability Exchange), legible por máquinas.

## 15. Vistas y resúmenes

Occtet ofrece varias vistas globales:

| Vista | Propósito |
| --- | --- |
| Inventory | Todos los InventoryItems |
| VEX Listing | Todas las decisiones VEX |
| Licenses | Todas las licencias del proyecto |
| SoftwareComponents | Componentes técnicos |
| Projects | Todos los proyectos |

Todas las listas pueden filtrarse, ordenarse y buscarse.

## 16. Exportación

Puedes exportar:

- SBOMs depuradas (incluyendo información legal relevante como CVEs, licencias y derechos de autor)
- documentos VEX

Esto permite:

- entregar resultados a clientes
- cumplir con requisitos de auditoría o autoridades

## 17. Integración

Gracias a su API y a la integración con ORT, Occtet Curator puede incorporarse en pipelines de construcción.
Esto permite ejecutar ORT runs automáticamente e importar los resultados directamente.

También se detectan ejecuciones iniciadas externamente, que pueden importarse fácilmente.

De este modo, puedes mantener tus SBOM actualizadas y evaluar continuamente las vulnerabilidades.

## 18. Proceso de auditoría – Revisión y curación de una SBOM

La importación de una SBOM constituye inicialmente una recopilación automatizada de los componentes de software detectados en el proyecto. Sin embargo, el resultado de un análisis puede ser incompleto: algunos componentes pueden no ser detectados por el scanner (que ha generado la SBOM), pueden estar asignados incorrectamente o pueden no disponer de información suficiente sobre licencias, derechos de autor o vulnerabilidades.

Por lo tanto, el objetivo del proceso de auditoría es comparar la información generada automáticamente con el contenido real del código fuente y completar la SBOM. El objetivo es crear un inventario de software lo más completo, trazable y auditable posible.

La revisión se realiza esencialmente siguiendo los siguientes pasos:

1. **Revisar el proyecto y el Filetree**
2. **Identificar y añadir componentes de software no detectados**
3. **Revisar los archivos y las indicaciones legales**
4. **Asignar la información a los InventoryItems y documentarla en las AuditNotes**
5. **Revisar y actualizar las vulnerabilidades de los componentes de software**
6. **Finalizar la revisión y marcar los archivos como reviewed**

### 18.1 Revisar el proyecto y el Filetree

Después de importar la SBOM, se examina primero el proyecto completo en SBOM Management. El Filetree situado a la izquierda y la cuadrícula de InventoryItems situada a la derecha sirven como puntos de partida para la revisión manual.

El objetivo es obtener una visión general de qué archivos forman realmente parte del proyecto y cuáles de ellos ya están asignados a un InventoryItem.

En particular, se deben comprobar los siguientes puntos:

- ¿Están presentes todos los archivos relevantes del proyecto?
- ¿Los archivos ya están asignados a un InventoryItem adecuado?
- ¿Existen archivos que todavía no estén asignados a ningún InventoryItem?
- ¿Existen InventoryItems para cuyos SoftwareComponents no se haya podido descargar ningún código?
- ¿Existen indicios de que el scanner no haya detectado un componente de software?
- ¿Contienen los archivos indicaciones de licencia o de derechos de autor que todavía no se hayan tenido en cuenta en la SBOM importada?

Una revisión completa del Filetree es especialmente importante en proyectos cuyas dependencias no pueden identificarse exclusivamente mediante el gestor de paquetes utilizado o el scanner automático.

Sin embargo, el Filetree puede seguir estando incompleto si no se ha podido descargar el código de un InventoryItem. Esto puede ocurrir, por ejemplo, si la URL de un SoftwareComponent es inválida, incorrecta o no es compatible con las estrategias de descarga del Curator. Actualmente, se admiten repositorios Git y Maven.

### 18.2 Identificar componentes de software no detectados

Una auditoría no se limita a revisar los InventoryItems proporcionados por el scanner/import. También es necesario determinar si existen componentes de software en el código fuente que no hayan sido detectados por el scanner.

Si, por ejemplo, se encuentran archivos que pertenecen claramente a un componente de terceros para el que todavía no existe ningún InventoryItem correspondiente, dicho componente debe añadirse manualmente a Occtet.

Para ello, primero se crea o selecciona un **SoftwareComponent** adecuado y, a continuación, se crea un **InventoryItem** correspondiente para el proyecto.

El InventoryItem constituye así el vínculo entre el componente de software técnico y el proyecto concreto. Solo mediante esta asociación puede evaluarse completamente el componente como parte de la auditoría del proyecto.

Al identificar componentes desconocidos, pueden ser útiles, entre otros, los siguientes indicios:

- Nombres de archivos y directorios
- Estructuras de paquetes y namespaces
- Encabezados de copyright y de licencia
- Archivos README o NOTICE
- Documentación incluida en el componente
- Información de versión
- Referencias a proveedores externos o proyectos externos
- Estructuras de archivos típicas o fragmentos de código fuente conocidos

La incorporación manual es especialmente importante, ya que una SBOM solo puede ser tan completa como el proceso de detección en el que se basa.

### 18.3 Revisar los archivos y la información legal

A continuación, se revisan en detalle los archivos asignados a los InventoryItems.

El Code Viewer permite abrir archivos, examinarlos y buscar términos relevantes. Además del propio código fuente, la información legal y relacionada con las licencias es especialmente relevante. Para ello, Occtet puede resaltar en el contenido de los archivos los términos de búsqueda configurados.

Durante la revisión se debe buscar especialmente la siguiente información:

- Encabezados de licencia
- SPDX-License-Identifier
- Indicaciones de copyright
- Archivos NOTICE y otras indicaciones legales
- Referencias a código de terceros
- Información sobre el origen del código
- Condiciones de uso adicionales
- Restricciones de exportación u otras restricciones legales

No basta con determinar simplemente la existencia de una licencia. También es importante determinar **qué parte del proyecto está cubierta por la licencia correspondiente** y comprobar si la información encontrada pertenece realmente al componente de software que se está examinando.

Por ejemplo, un repositorio puede contener varios componentes con diferentes licencias. Del mismo modo, algunos archivos pueden tener su propio encabezado de licencia, mientras que para un componente de nivel superior se documenta una licencia diferente.

### 18.4 Documentar la información en el InventoryItem

La información obtenida durante la revisión de los archivos se consolida posteriormente en el InventoryItem correspondiente.

Para cada componente de software, debería poder rastrearse especialmente la siguiente información:

- qué archivos pertenecen al componente,
- qué licencia se ha identificado para el componente o para los archivos asociados,
- qué indicaciones de copyright están presentes,
- dónde se encontró esta información en el proyecto,
- qué decisiones manuales se tomaron como parte de la auditoría.

Las **AuditNotes** sirven como evidencia y trazabilidad de las decisiones tomadas. Deben describir de la forma más concreta posible **qué se ha identificado y dónde se puede encontrar la evidencia correspondiente**.

Ejemplo:

*«MIT License encontrada en el archivo LICENSE del directorio /thirdparty/example. Indicación de copyright encontrada en el encabezado de example.java. Archivos asignados al InventoryItem Example Library 2.4.»*

Esto permite comprender posteriormente sobre qué base se incorporó la información relativa a la licencia y al copyright.

Por lo tanto, las AuditNotes no son simplemente una nota personal del Curator. Proporcionan la justificación de las decisiones manuales y, de este modo, sirven de apoyo para auditorías posteriores, consultas de clientes y revisiones legales.

### 18.5 Evaluar las licencias y los copyrights

La información sobre licencias y copyrights encontrada en los archivos se compara con la información ya identificada por el scanner/import.

Pueden darse, por ejemplo, las siguientes situaciones:

- La licencia detectada automáticamente queda confirmada por un encabezado de licencia inequívoco.
- Se ha detectado automáticamente una licencia, pero esta no puede verificarse claramente en el código fuente.
- Se encuentra una licencia adicional en un archivo que no había sido tenido en cuenta por el scanner.
- Se encuentra una indicación de copyright en el código fuente y debe asignarse al componente.
- Existen varias indicaciones de licencia que requieren una evaluación más detallada.

En caso de información contradictoria o poco clara, la decisión no debería tomarse sin justificación. En su lugar, la información en la que se basa la decisión debe documentarse en las AuditNotes y, si es necesario, revisarse con mayor profundidad.

Especialmente en el caso de componentes Open Source, es importante distinguir entre la **identificación técnica de una licencia** y la **evaluación jurídica de sus implicaciones**. Occtet permite recopilar esta información y garantizar su trazabilidad; la evaluación jurídica propiamente dicha puede requerir una revisión adicional en función del proyecto y de la licencia correspondiente.

### 18.6 Revisar las vulnerabilidades de los componentes de software

Una vez comprobadas la identidad y la asignación de los componentes de software, se revisan sus vulnerabilidades conocidas.

Occtet asigna inicialmente las vulnerabilidades de forma automática a los SoftwareComponents. Durante la auditoría se comprueba posteriormente si esta asignación es relevante para el uso concreto del componente en el proyecto.

Para cada vulnerabilidad relevante se deben comprobar especialmente los siguientes puntos:

- ¿Está realmente presente en el proyecto el componente de software afectado?
- ¿La versión detectada corresponde a la versión que realmente se está utilizando?
- ¿La vulnerabilidad es relevante para esta versión?
- ¿Se utiliza realmente en el proyecto la parte vulnerable del componente?
- ¿La vulnerabilidad ya ha sido corregida o eliminada mediante una versión más reciente?
- ¿La vulnerabilidad es realmente explotable en el contexto específico del proyecto o de su uso?

Los resultados de esta revisión se actualizan posteriormente en la vista del InventoryItem para las Vulnerabilities correspondientes.

Por ejemplo, pueden utilizarse los siguientes estados:

- **Affected** – el componente está afectado por la vulnerabilidad.
- **Not Affected** – la vulnerabilidad no afecta al uso concreto del componente.
- **Under Investigation** – la evaluación todavía no se ha completado.
- **Fixed** – la vulnerabilidad, por ejemplo, se ha solucionado mediante una actualización.

Estas evaluaciones pueden exportarse posteriormente como información VEX.

Es importante distinguir entre una **vulnerabilidad conocida de un componente** y el **riesgo real para el proyecto concreto**. La presencia de una CVE no significa automáticamente que la aplicación sea explotable. Para realizar una evaluación fiable deben tenerse en cuenta, entre otros factores, la versión, la funcionalidad afectada y el uso concreto del componente.

### 18.7 InventoryItem como unidad central de auditoría

El InventoryItem constituye, por tanto, la unidad central de la auditoría. En él se agrupan las diferentes informaciones relativas a un componente de software:

- SoftwareComponent y versión
- Archivos asociados
- Licencias
- Copyrights
- Vulnerabilidades
- Evaluaciones VEX
- AuditNotes

El objetivo es que un auditor u otro Curator pueda utilizar el InventoryItem para comprender **qué componente se utiliza, de qué archivos está compuesto, qué información legal está disponible y cómo se han evaluado los aspectos relacionados con la seguridad**.

### 18.8 Finalizar la auditoría

Un archivo solo debería marcarse como **reviewed** una vez que se hayan verificado su asignación y la información relevante.

Para considerar una revisión como completada, se debe garantizar especialmente lo siguiente:

- El archivo está asignado al InventoryItem correcto.
- El componente de software ha sido identificado de forma inequívoca.
- Se ha revisado la información relativa a la licencia.
- Se ha revisado la información relativa al copyright.
- Se ha tenido en cuenta la información legal adicional relevante.
- Las vulnerabilidades detectadas han sido revisadas y evaluadas.
- Las decisiones manuales han sido documentadas en las AuditNotes.

Este proceso se repite para los archivos y componentes de software relevantes del proyecto.

Al finalizar la auditoría, la SBOM importada ya no debería representar simplemente una lista de componentes generada automáticamente, sino un **inventario de software verificado y curado de forma trazable**.

La información obtenida de este modo constituye posteriormente la base para las evaluaciones de seguridad, los análisis de licencias y copyrights, así como para la creación de informes VEX y de una SBOM depurada.
