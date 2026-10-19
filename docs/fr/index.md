# Occtet Curator – Guide utilisateur

## 1. Introduction

### 1.1 Qu’est-ce que Occtet Curator ?

Occtet Curator est une plateforme professionnelle dédiée à l’analyse, à la curation et à l’évaluation des SBOM (Software Bill of Materials), ainsi qu’à la création de rapports VEX.

Une SBOM décrit l’ensemble des composants logiciels qui constituent un projet. Ces informations servent de base pour :

- l’analyse de sécurité (CVE, vulnérabilités)
- la conformité des licences
- l’analyse des copyrights
- l’évaluation des risques dans la chaîne d’approvisionnement logicielle

Occtet Curator transforme une SBOM brute en unités structurées et vérifiables, facilitant ainsi leur analyse par des utilisateurs.
L’élément central de ce processus est l’**InventoryItem**.

### 1.2 Concepts clés

| Terme | Description |
| --- | --- |
| SBOM | Liste de tous les composants logiciels d’un projet (par exemple SPDX). |
| InventoryItem | Représentation structurée d’un composant logiciel, incluant les fichiers, licences, copyrights et CVE associés. |
| SoftwareComponent | Le composant logiciel lui-même (par exemple OpenSSL 3.0.2). |
| Vulnerability / CVE | Identifiant standard indiquant si un composant logiciel est affecté par une vulnérabilité connue. |
| VEX | Indique si une vulnérabilité (CVE) est pertinente pour le projet. |
| AuditNote | Documentation permettant de justifier et tracer les décisions prises. |

## 2. Principe de fonctionnement : travailler avec Occtet

Occtet Curator repose sur une organisation par projets. Une organisation peut contenir plusieurs projets et utilisateurs.

L’administrateur est responsable de :

- la création des organisations
- l’attribution des utilisateurs aux organisations
- la définition du chemin système où le code source est stocké

Le curator suit généralement les étapes suivantes :

1. Créer un projet (Projects)
2. Importer une SBOM (Import → SPDX Import ou via ORT Starter si une instance ORT est disponible)
3. Accéder à la gestion SBOM (SBOMmgmt), sélectionner le projet et commencer la curation :
   - InventoryItems à droite
   - arborescence des fichiers à gauche
4. Utiliser la vue Overview pour examiner les vulnérabilités, licences et copyrights
5. Travailler avec les InventoryItems :
   - ils contiennent des informations détaillées sur les SoftwareComponents
   - incluent les vulnérabilités, licences et copyrights
   - regroupent les fichiers associés ou des fichiers indépendants partageant des caractéristiques communes (par exemple une même licence)
   - permettent d’ajouter des AuditNotes pour documenter les preuves et décisions
6. Créer et télécharger des rapports VEX pour les vulnérabilités dans le contexte d’un InventoryItem
7. Télécharger la SBOM enrichie depuis la page du projet

Le système est conçu pour transformer une SBOM brute en un inventaire logiciel vérifié et prêt pour audit.

Les sections suivantes décrivent en détail les différentes parties du système.

## 3. Configuration (Admin)

### 3.1 Définir le chemin système

Le chemin système détermine l’emplacement où le code source des projets est stocké.
Il est nécessaire pour afficher le contenu des fichiers dans la gestion SBOM.
Sans chemin système, aucun projet ne peut être créé.

### 3.2 Autocomplétion de texte (suggestions)

Par défaut, certaines suggestions d’autocomplétion sont déjà disponibles.
Elles peuvent être utilisées dans les AuditNotes ou comme nom d’InventoryItem.

Seul l’administrateur peut en ajouter de nouvelles.
Actuellement, deux contextes spécifiques sont pris en charge et doivent être utilisés tels quels, car ils sont implémentés uniquement pour ces cas d’usage.

### 3.3 Termes de recherche

Des profils de termes de recherche peuvent être définis afin de mettre en évidence certains mots-clés dans le contenu des fichiers.

Exemples courants : itext, gpl, lgpl, stackoverflow.
Les termes doivent être saisis sous forme de liste séparée par des virgules.

### 3.4 Api Key

L'administrateur peut générer une clé API qui peut être utilisée pour l'intégration avec Vulnerable Code.
Cela est nécessaire pour obtenir des données sur les vulnérabilités.
La clé est fournie par Vulnerable Code à l'adresse suivante : <https://public.vulnerablecode.io/account/request_api_key/>

## 4. Organisation (Admin)

L’administrateur crée des organisations et y associe des utilisateurs.
Il peut également attribuer des projets aux organisations.
Chaque utilisateur, qu'il s'agisse d'un administrateur ou d'un modérateur,
doit être rattaché à une organisation pour pouvoir utiliser toutes les fonctionnalités.

- Lorsqu’un administrateur crée un projet, il peut choisir l’organisation associée
- Lorsqu’un curator crée un projet, celui-ci est automatiquement rattaché à son organisation

## 5. Projets

### 5.1 Création d’un projet

Les informations suivantes sont obligatoires :

- Nom
- Version
- Responsable produit

Champs optionnels :

- Adresse e-mail de contact
- Profil de recherche

Règles d’attribution :

- Projet créé par un administrateur : choix de l’organisation
- Projet créé par un curator : attribution automatique

Les données des projets au sein d’une même organisation peuvent être réutilisées.
Les licences et copyrights ne sont pas liés à une organisation spécifique et peuvent être utilisés globalement.

## 6. Importer une SBOM

Vous pouvez importer une SBOM (actuellement au format SPDX) via la fonction de téléversement de fichier.
Lors de l’import, vous pouvez également configurer l’utilisation de workflows basés sur l’IA pour :

- la détection des copyrights
- la classification des licences

Après l’import, Occtet génère automatiquement :

- les SoftwareComponents
- les InventoryItems
- les vulnérabilités (CVE)
- les fichiers
- les licences
- les copyrights

## 7. Lancer un ORT Run

La page d’import propose également un bouton permettant de lancer un ORT run. Pour utiliser cette fonctionnalité, une instance ORT (Docker) en cours d’exécution doit être connectée au système. Les paramètres de connexion peuvent être définis dans le fichier `application.properties` ou dynamiquement via un fichier `.env`.

Lors de la configuration d’un ORT run, vous devez définir :

- le type de dépôt à analyser
- l’URL du dépôt
- la version à analyser

## 8. Tableau de bord

Le tableau de bord agit comme un indicateur de sécurité de votre projet. Il affiche notamment :

- le nombre de CVE classées par niveau de criticité (Critical, High, Medium, Low)
- le nombre de composants logiciels affectés par des vulnérabilités

Cela vous permet d’évaluer rapidement si un projet est prêt pour une mise en production ou s’il présente un risque.

## 9. Gestion de la SBOM

Il s’agit de votre espace de travail principal. L’interface est organisée comme suit :

- à gauche : l’arborescence des fichiers
- à droite : les InventoryItems
- au centre : une vue d’ensemble (vulnérabilités, licences, copyrights)

Idéalement, chaque fichier doit être associé à un seul InventoryItem.

## 10. InventoryItems – Élément central

Un InventoryItem représente un composant logiciel et contient :

- le SoftwareComponent (nom, version, URL d’origine)
- les fichiers associés
- les vulnérabilités (CVE) (avec possibilité de créer des entrées VEX)
- les licences
- les copyrights
- les AuditNotes pour documenter les décisions

Les InventoryItems constituent la base de votre analyse. Ils permettent de structurer la SBOM en unités exploitables et vérifiables.

## 11. SoftwareComponents

Les composants logiciels sont créés automatiquement lors de la lecture de la SBOM. Chaque composant logiciel est associé à un élément d'inventaire.
Il est également possible de créer manuellement des composants logiciels. Cela peut se faire directement dans la gestion de la SBOM, au niveau de l'élément d'inventaire
ou dans la vue « Composants logiciels ». Le nom et la version sont des champs obligatoires. Si vous créez le composant dans la vue « Composants logiciels » ,
vous devez également créer un élément d'inventaire correspondant et y associer le composant logiciel.
L'InventoryItem constitue le lien avec un projet. Sans InventoryItem, le SoftwareComponent ne peut être affecté à aucun projet.

## 12. Travailler avec les fichiers

### 12.1 Visualiseur de code

Pour chaque fichier, vous pouvez :

- l’ouvrir
- le lire
- effectuer des recherches
- l’associer à un InventoryItem

Lors de l’analyse, vous pouvez identifier des informations juridiques importantes telles que :

- des en-têtes de licence
- des mentions de copyright
- des références à du code tiers

Ces éléments doivent être documentés dans les AuditNotes afin d’assurer la traçabilité de vos décisions.
Par exemple, vous pouvez expliquer pourquoi un fichier a été associé à un InventoryItem spécifique ou pourquoi une licence est jugée acceptable.

Les restrictions d’exportation ou autres mentions légales identifiées dans le contenu des fichiers doivent également être consignées dans les AuditNotes.
Les termes de recherche définis dans votre profil sont mis en évidence dans le contenu des fichiers.

### 12.2 Statut « Reviewed »

Un fichier est considéré comme vérifié lorsque :

- il est associé au bon InventoryItem
- sa licence a été validée
- ses copyrights ont été examinés
- toutes les conclusions ont été documentées

Une fois ces étapes réalisées, le fichier peut être marqué comme **reviewed**.

## 13. AuditNotes

Les AuditNotes constituent votre documentation et votre preuve. Elles permettent notamment d’expliquer :

- pourquoi une licence est jugée acceptable
- pourquoi une CVE est considérée comme non pertinente
- pourquoi un copyright peut être ignoré

Elles sont essentielles pour :

- les audits de conformité (par exemple ISO)
- les demandes clients et la transparence
- la documentation juridique

## 14. Vulnérabilités & VEX

Occtet identifie automatiquement les CVE associées à chaque SoftwareComponent. Vous évaluez ensuite leur pertinence via les statuts VEX :

- Affected
- Not Affected
- Under Investigation
- Fixed

Ces évaluations peuvent être exportées au format VEX (Vulnerability Exploitability Exchange), lisible par machine.

Dans la vue détaillée d’un InventoryItem (dans la gestion SBOM), vous pouvez créer des déclarations VEX individuellement pour chaque CVE ou de manière groupée.

## 15. Vues & Aperçus

Occtet propose plusieurs vues globales :

| Vue | Objectif |
| --- | --- |
| Inventory | Tous les InventoryItems |
| VEX Listing | Toutes les décisions VEX |
| Licenses | Toutes les licences du projet |
| SoftwareComponents | Composants techniques |
| Projects | Tous les projets |

Toutes les listes peuvent être filtrées, triées et recherchées.

## 16. Export

Vous pouvez exporter :

- des SBOM nettoyées (incluant les informations juridiques pertinentes telles que CVE, licences et copyrights)
- des documents VEX

Ces exports peuvent être utilisés pour :

- fournir des livrables aux clients
- répondre aux exigences des autorités ou des auditeurs

## 17. Intégration

Grâce à son API et à son intégration avec ORT, Occtet Curator peut être intégré dans une pipeline de build. Cela permet :

- de lancer automatiquement des ORT runs
- d’importer directement les résultats dans Occtet Curator

Les ORT runs lancés en externe sont également détectés par le Curator et peuvent être importés facilement.

Vous pouvez ainsi maintenir vos SBOM à jour en continu et évaluer les vulnérabilités de manière systématique.

## 18. Processus d’audit – Vérification et curation d’une SBOM

L’importation d’une SBOM constitue tout d’abord une collecte automatisée des composants logiciels détectés dans le projet. Le résultat d’un scan peut toutefois être incomplet : certains composants peuvent ne pas être détectés par le scanner (qui a généré la SBOM), être mal attribués ou ne pas être accompagnés d’informations suffisantes concernant les licences, les droits d’auteur ou les vulnérabilités.

Le processus d’audit vise donc à comparer les informations générées automatiquement avec le contenu réel du code source et à compléter la SBOM. L’objectif est d’obtenir un inventaire logiciel aussi complet, traçable et auditable que possible.

La vérification s’effectue essentiellement selon les étapes suivantes :

1. **Vérifier le projet et le Filetree**
2. **Identifier et ajouter les composants logiciels non détectés**
3. **Vérifier les fichiers et les mentions légales**
4. **Associer les informations aux InventoryItems et les documenter dans les AuditNotes**
5. **Vérifier et mettre à jour les vulnérabilités des composants logiciels**
6. **Finaliser la vérification et marquer les fichiers comme reviewed**

### 18.1 Vérifier le projet et le Filetree

Après l’importation de la SBOM, l’ensemble du projet est d’abord examiné dans le SBOM Management. Le Filetree situé à gauche et la grille des InventoryItems située à droite servent de points de départ pour la vérification manuelle.

L’objectif est d’obtenir une vue d’ensemble des fichiers qui font réellement partie du projet et de déterminer lesquels sont déjà associés à un InventoryItem.

Il convient notamment de vérifier les points suivants :

- Tous les fichiers pertinents du projet sont-ils présents ?
- Les fichiers sont-ils déjà associés à un InventoryItem approprié ?
- Existe-t-il des fichiers qui ne sont encore associés à aucun InventoryItem ?
- Existe-t-il des InventoryItems pour lesquels aucun code n’a pu être téléchargé pour leur SoftwareComponent ?
- Existe-t-il des indices indiquant que le scanner n’a pas détecté un composant logiciel ?
- Les fichiers contiennent-ils des mentions de licence ou de copyright qui n’ont pas encore été prises en compte dans la SBOM importée ?

Une vérification complète du Filetree est particulièrement importante pour les projets dont les dépendances ne peuvent pas être identifiées exclusivement à l’aide du gestionnaire de paquets utilisé ou du scanner automatique.

Le Filetree peut néanmoins être incomplet lorsqu’aucun code n’a pu être téléchargé pour un InventoryItem. Cela peut par exemple se produire lorsque l’URL d’un SoftwareComponent est invalide, incorrecte ou incompatible avec les stratégies de téléchargement du Curator. Actuellement, les dépôts Git et Maven sont pris en charge.

### 18.2 Identifier les composants logiciels non détectés

Un audit ne se limite pas à vérifier les InventoryItems fournis par le scanner/import. Il faut également déterminer si des composants logiciels sont présents dans le code source sans avoir été détectés par le scanner.

Si, par exemple, des fichiers sont clairement identifiés comme appartenant à un composant tiers pour lequel aucun InventoryItem correspondant n’existe encore, ce composant doit être ajouté manuellement dans Occtet.

Pour cela, un **SoftwareComponent** approprié est d’abord créé ou sélectionné, puis un **InventoryItem** correspondant est créé pour le projet.

L’InventoryItem constitue ainsi le lien entre le composant logiciel technique et le projet concret. Ce n’est qu’à travers cette association que le composant peut être évalué de manière complète dans le cadre de l’audit du projet.

Lors de l’identification de composants inconnus, les indices suivants peuvent notamment être utiles :

- Noms de fichiers et de répertoires
- Structures de packages et de namespaces
- En-têtes de copyright et de licence
- Fichiers README ou NOTICE
- Documentation présente au sein du composant
- Informations de version
- Références à des fournisseurs tiers ou à des projets externes
- Structures de fichiers caractéristiques ou fragments de code source connus

L’ajout manuel est particulièrement important, car une SBOM ne peut être aussi complète que le processus de détection sur lequel elle repose.

### 18.3 Vérifier les fichiers et les informations légales

Les fichiers associés aux InventoryItems sont ensuite examinés en détail.

Le Code-Viewer permet d’ouvrir les fichiers, de les parcourir et de rechercher des termes pertinents. Outre le code source lui-même, les informations juridiques et relatives aux licences sont particulièrement importantes. Occtet peut mettre en évidence dans le contenu des fichiers les termes de recherche configurés.

Lors de la vérification, il convient notamment de rechercher les informations suivantes :

- En-têtes de licence
- SPDX-License-Identifier
- Mentions de copyright
- Fichiers NOTICE et autres mentions légales
- Références à du code provenant de tiers
- Informations sur l’origine du code
- Conditions d’utilisation supplémentaires
- Restrictions à l’exportation ou autres restrictions légales

Il ne suffit pas de constater l’existence d’une licence. Il est également essentiel de déterminer **quelle partie du projet est concernée par la licence correspondante** et de vérifier que l’information trouvée concerne effectivement le composant logiciel examiné.

Par exemple, un repository peut contenir plusieurs composants soumis à des licences différentes. De même, certains fichiers peuvent posséder leur propre en-tête de licence, tandis qu’une autre licence est documentée pour un composant de niveau supérieur.

### 18.4 Documenter les informations dans l’InventoryItem

Les informations obtenues lors de l’examen des fichiers sont ensuite regroupées dans l’InventoryItem correspondant.

Pour chaque composant logiciel, les informations suivantes doivent notamment pouvoir être retracées :

- quels fichiers appartiennent au composant,
- quelle licence a été identifiée pour le composant ou les fichiers associés,
- quelles mentions de copyright sont présentes,
- où ces informations ont été trouvées dans le projet,
- quelles décisions manuelles ont été prises dans le cadre de l’audit.

Les **AuditNotes** servent de preuve et de traçabilité pour les décisions prises. Elles doivent décrire de manière aussi précise que possible **ce qui a été constaté et où se trouve la preuve correspondante**.

Exemple :

*« MIT License trouvée dans le fichier LICENSE du répertoire /thirdparty/example. Mention de copyright trouvée dans l’en-tête de example.java. Fichiers associés à l’InventoryItem Example Library 2.4. »*

Cela permet de comprendre ultérieurement sur quelle base les informations relatives à la licence et au copyright ont été reprises.

Les AuditNotes ne constituent donc pas uniquement une note personnelle du Curator. Elles fournissent la justification des décisions manuelles et facilitent ainsi les audits ultérieurs, les demandes des clients et les vérifications juridiques.

### 18.5 Évaluer les licences et les copyrights

Les informations relatives aux licences et aux copyrights trouvées dans les fichiers sont comparées aux informations déjà identifiées par le scanner/import.

Les situations suivantes peuvent notamment se présenter :

- La licence détectée automatiquement est confirmée par un en-tête de licence explicite.
- Une licence a été détectée automatiquement, mais ne peut pas être clairement vérifiée dans le code source.
- Une licence supplémentaire est trouvée dans un fichier qui n’avait pas été pris en compte par le scanner.
- Une mention de copyright est trouvée dans le code source et doit être associée au composant.
- Plusieurs indications de licence existent et doivent faire l’objet d’une évaluation plus approfondie.

En cas d’informations contradictoires ou peu claires, la décision ne doit pas être prise sans justification. Les informations sur lesquelles repose la décision doivent au contraire être documentées dans les AuditNotes et, si nécessaire, faire l’objet d’une vérification complémentaire.

Dans le cas des composants Open Source en particulier, il convient de distinguer **l’identification technique d’une licence** de **l’évaluation juridique de ses conséquences**. Occtet permet de collecter et d’assurer la traçabilité de ces informations ; l’évaluation juridique proprement dite peut nécessiter une analyse supplémentaire en fonction du projet et de la licence concernée.

### 18.6 Vérifier les vulnérabilités des composants logiciels

Une fois l’identité et l’association des composants logiciels vérifiées, leurs vulnérabilités connues sont examinées.

Occtet associe dans un premier temps automatiquement les vulnérabilités aux SoftwareComponents. Lors de l’audit, il convient ensuite de vérifier si cette association est pertinente pour l’utilisation concrète du composant dans le projet.

Pour chaque vulnérabilité pertinente, il convient notamment de vérifier les points suivants :

- Le composant logiciel concerné est-il effectivement présent dans le projet ?
- La version détectée correspond-elle à la version réellement utilisée ?
- La vulnérabilité concernée est-elle pertinente pour cette version ?
- La partie vulnérable du composant est-elle effectivement utilisée dans le projet ?
- La vulnérabilité a-t-elle déjà été corrigée ou éliminée par une version plus récente ?
- La vulnérabilité est-elle effectivement exploitable dans le contexte concret du projet ou de son utilisation ?

Les résultats de cette vérification sont ensuite mis à jour dans la vue de l’InventoryItem au niveau des Vulnerabilities correspondantes.

Les états suivants peuvent notamment être utilisés :

- **Affected** – le composant est affecté par la vulnérabilité.
- **Not Affected** – la vulnérabilité ne concerne pas l’utilisation concrète du composant.
- **Under Investigation** – l’évaluation n’est pas encore terminée.
- **Fixed** – la vulnérabilité a par exemple été corrigée par une mise à jour.

Ces évaluations peuvent ensuite être exportées sous forme d’informations VEX.

Il est important de distinguer **une vulnérabilité connue d’un composant** du **risque réel pour le projet concerné**. La présence d’une CVE ne signifie pas automatiquement que l’application est exploitable. Une évaluation fiable doit notamment prendre en compte la version, la fonctionnalité concernée et l’utilisation concrète du composant.

### 18.7 L’InventoryItem comme unité centrale de l’audit

L’InventoryItem constitue ainsi l’unité centrale de l’audit. Il regroupe les différentes informations relatives à un composant logiciel :

- SoftwareComponent et version
- fichiers associés
- licences
- copyrights
- vulnérabilités
- évaluations VEX
- AuditNotes

L’objectif est qu’un auditeur ou un autre Curator puisse, à partir de l’InventoryItem, comprendre **quel composant est utilisé, de quels fichiers il se compose, quelles informations juridiques sont disponibles et comment les éléments liés à la sécurité ont été évalués**.

### 18.8 Finaliser l’audit

Un fichier ne devrait être marqué comme **reviewed** qu’une fois que son association et les informations pertinentes ont été vérifiées.

Pour qu’une vérification soit considérée comme terminée, il convient notamment de s’assurer que :

- Le fichier est associé au bon InventoryItem.
- Le composant logiciel a été identifié sans ambiguïté.
- Les informations relatives à la licence ont été vérifiées.
- Les informations relatives au copyright ont été vérifiées.
- Les éventuelles informations légales supplémentaires pertinentes ont été prises en compte.
- Les vulnérabilités détectées ont été vérifiées et évaluées.
- Les décisions manuelles ont été documentées dans les AuditNotes.

Ce processus est répété pour les fichiers et composants logiciels pertinents du projet.

À la fin de l’audit, la SBOM importée ne doit plus être considérée comme une simple liste de composants générée automatiquement, mais comme un **inventaire logiciel vérifié et curaté de manière traçable**.

Les informations ainsi obtenues constituent ensuite la base des évaluations de sécurité, des analyses de licences et de copyrights ainsi que de la création de rapports VEX et d’une SBOM nettoyée.
