# Occtet Curator – Guida utente

## 1. Introduzione

### 1.1 Che cos’è Occtet Curator?

Occtet Curator è una piattaforma professionale progettata per analizzare, curare e valutare le SBOM (Software Bill of Materials), nonché per generare report VEX.

Una SBOM descrive tutti i componenti software che costituiscono un progetto. Queste informazioni sono fondamentali per:

- Analisi della sicurezza (CVE e vulnerabilità)
- Conformità delle licenze
- Analisi dei diritti d’autore
- Valutazione dei rischi nella supply chain del software

Occtet Curator trasforma una SBOM grezza in unità strutturate e verificabili, rendendole facilmente analizzabili.
L’elemento centrale di questo processo è l’**InventoryItem**.

### 1.2 Concetti chiave

| Termine | Descrizione |
| --- | --- |
| SBOM | Elenco di tutti i componenti software di un progetto (ad es. SPDX). |
| InventoryItem | Rappresentazione strutturata di un componente software, inclusi file, licenze, diritti d’autore e CVE. |
| SoftwareComponent | Il componente software vero e proprio (ad es. OpenSSL 3.0.2). |
| Vulnerability / CVE | Identificatore standard che indica se un componente software è affetto da una vulnerabilità nota. |
| VEX | Indica se una vulnerabilità (CVE) è rilevante per il progetto. |
| AuditNote | Documentazione utilizzata per giustificare e tracciare le decisioni. |

## 2. Flusso di lavoro: come utilizzare Occtet

Occtet Curator adotta un approccio basato su progetti. Un’organizzazione può includere più utenti e progetti.

L’amministratore è responsabile di:

- Creare le organizzazioni
- Assegnare gli utenti alle organizzazioni
- Definire il percorso di sistema in cui viene archiviato il codice sorgente

Il curatore lavora generalmente nel seguente modo:

1. Creare un progetto (Projects)
2. Importare una SBOM (Import → SPDX Import oppure tramite ORT Starter se disponibile un’istanza ORT)
3. Accedere a SBOM Management (SBOMmgmt), selezionare il progetto e iniziare la curation:
   - InventoryItems a destra
   - Struttura dei file a sinistra
4. Utilizzare la vista Overview per esaminare vulnerabilità, licenze e diritti d’autore
5. Lavorare con gli InventoryItems:
   - Contengono informazioni dettagliate sui SoftwareComponents
   - Includono vulnerabilità, licenze e diritti d’autore
   - Raggruppano file associati o file indipendenti con caratteristiche comuni (ad es. stessa licenza)
   - Consentono di aggiungere AuditNotes per documentare decisioni ed evidenze
6. Creare e scaricare report VEX nel contesto di un InventoryItem
7. Scaricare la SBOM curata dalla pagina del progetto

Il sistema è progettato per trasformare una SBOM grezza in un inventario software verificato e pronto per audit.

Le sezioni seguenti descrivono in dettaglio le varie funzionalità.

## 3. Configurazione (Admin)

### 3.1 Definire il percorso di sistema

Il percorso di sistema determina dove viene archiviato il codice sorgente dei progetti.
È necessario per visualizzare il contenuto dei file in SBOM Management.
Senza questo percorso, non è possibile creare progetti.

### 3.2 Autocompletamento del testo (suggerimenti)

Per impostazione predefinita sono disponibili alcuni suggerimenti per l’autocompletamento.
Possono essere utilizzati nelle AuditNotes o come nome degli InventoryItems.

Solo l’amministratore può crearne di nuovi.
Attualmente esistono due contesti specifici che devono essere utilizzati come definiti.

### 3.3 Termini di ricerca

È possibile definire profili di termini di ricerca per evidenziare parole chiave nei contenuti dei file.

Esempi comuni: itext, gpl, lgpl, stackoverflow.
I termini devono essere inseriti come elenco separato da virgole.

### 3.4 Api Key

L'amministratore può generare una chiave API da utilizzare per l'integrazione con Vulnerable Code.
Ciò è necessario per ricevere i dati relativi alle vulnerabilità.
La chiave viene ottenuta da Vulnerable Code all'indirizzo: <https://public.vulnerablecode.io/account/request_api_key/>

## 4. Organizzazione (Admin)

L’amministratore crea le organizzazioni e assegna loro gli utenti.
Può inoltre associare i progetti alle organizzazioni.
Ogni utente, che sia amministratore o curatore,
deve essere assegnato a un'organizzazione per poter utilizzare tutte le funzioni.

- Se un amministratore crea un progetto, può selezionare l’organizzazione
- Se un curatore crea un progetto, questo viene assegnato automaticamente alla sua organizzazione

## 5. Progetti

### 5.1 Creare un progetto

I seguenti campi sono obbligatori:

- Nome
- Versione
- Responsabile del prodotto

Campi opzionali:

- Email di contatto
- Profilo di ricerca

I dati dei progetti all’interno di un’organizzazione possono essere riutilizzati.
Le licenze e i diritti d’autore non sono legati a una specifica organizzazione e possono essere utilizzati globalmente.

## 6. Importare una SBOM

Puoi importare una SBOM (attualmente in formato SPDX) tramite la funzione di caricamento file. Durante l’importazione puoi anche configurare l’uso di flussi di lavoro basati su IA per:

- rilevamento dei diritti d’autore
- classificazione delle licenze

Dopo l’importazione, Occtet genera automaticamente:

- SoftwareComponents
- InventoryItems
- Vulnerabilità (CVE)
- File
- Licenze
- Diritti d’autore

## 7. Avviare un ORT Run

Nella pagina di importazione è disponibile anche un pulsante per avviare un ORT run. Per utilizzare questa funzionalità, è necessario che un’istanza ORT (Docker) sia in esecuzione e collegata al sistema. I parametri di connessione possono essere configurati nel file `application.properties` oppure tramite un file `.env`.

Durante la configurazione di un ORT run, è necessario specificare:

- il tipo di repository da analizzare
- l’URL del repository
- la versione

## 8. Dashboard

La dashboard funge da indicatore dello stato di sicurezza del progetto. Mostra, tra le altre cose:

- il numero di CVE per livello di gravità (Critical, High, Medium, Low)
- quanti componenti software sono affetti da vulnerabilità

Questo consente di valutare rapidamente se un progetto è pronto per il rilascio o rappresenta un rischio.

## 9. Gestione SBOM

Questo è il tuo spazio di lavoro principale:

- sinistra: struttura dei file
- destra: InventoryItems
- centro: Overview (vulnerabilità, licenze, diritti d’autore)

Idealmente, ogni file dovrebbe essere assegnato a un solo InventoryItem.

## 10. InventoryItems – Elemento centrale

Un InventoryItem rappresenta un componente software e include:

- SoftwareComponent (nome, versione, URL di origine)
- file associati
- vulnerabilità (CVE) (con possibilità di creare voci VEX)
- licenze
- diritti d’autore
- AuditNotes per documentare le decisioni

Gli InventoryItems sono la base dell’analisi e permettono di suddividere la SBOM in unità gestibili.

## 11. SoftwareComponents

I componenti software vengono creati automaticamente durante la lettura della SBOM.
Ogni componente software appartiene a un articolo di inventario.
È possibile creare i componenti software anche manualmente.
Questa operazione può essere eseguita direttamente nella gestione SBOM,
nella vista Articolo di inventario o nella vista Componente software.
Il nome e la versione sono campi obbligatori. Se si crea il componente nella vista Componente software,
è necessario creare anche un articolo di inventario e assegnarvi il componente software.
L'InventoryItem rappresenta il collegamento a un progetto.
Senza l'InventoryItem, la SoftwareComponent non può essere assegnata a nessun progetto.
Assegnare la SoftwareComponent a quest'ultimo.

## 12. Lavorare con i file

### 12.1 Visualizzatore di codice

Puoi:

- aprire i file
- leggerli
- cercare al loro interno
- assegnarli a un InventoryItem

Durante l’analisi possono emergere informazioni rilevanti come:

- intestazioni di licenza
- avvisi di copyright
- riferimenti a codice di terze parti

Queste informazioni devono essere documentate nelle AuditNotes per garantire la tracciabilità delle decisioni.
Eventuali restrizioni all’esportazione o altre note legali devono essere anch’esse registrate.
I termini di ricerca definiti nel profilo vengono evidenziati nel contenuto.

### 12.2 Stato “Reviewed”

Un file è considerato revisionato quando:

- è assegnato correttamente
- la licenza è stata verificata
- i diritti d’autore sono stati esaminati
- tutte le informazioni sono state documentate

Una volta completato, viene contrassegnato come **reviewed**.

## 13. AuditNotes

Le AuditNotes rappresentano la documentazione e la prova delle decisioni. Consentono di registrare, ad esempio:

- perché una licenza è accettabile
- perché una CVE non è rilevante
- perché un copyright può essere ignorato

Sono fondamentali per:

- audit di conformità (ad es. ISO)
- trasparenza verso i clienti
- documentazione legale

## 14. Vulnerabilità e VEX

Occtet rileva automaticamente le CVE associate a ciascun SoftwareComponent. Puoi valutarle tramite stati VEX:

- Affected
- Not Affected
- Under Investigation
- Fixed

Queste valutazioni possono essere esportate nel formato VEX (Vulnerability Exploitability Exchange), leggibile automaticamente.

## 15. Viste e panoramiche

Occtet offre diverse viste globali:

| Vista | Scopo |
| --- | --- |
| Inventory | Tutti gli InventoryItems |
| VEX Listing | Tutte le decisioni VEX |
| Licenses | Tutte le licenze del progetto |
| SoftwareComponents | Componenti tecnici |
| Projects | Tutti i progetti |

Tutte le liste possono essere filtrate, ordinate e ricercate.

## 16. Esportazione

Puoi esportare:

- SBOM pulite (incluse informazioni rilevanti come CVE, licenze e diritti d’autore)
- documenti VEX

Questo ti permette di:

- fornire risultati ai clienti
- soddisfare requisiti di audit o normativi

## 17. Integrazione

Grazie all’API e all’integrazione con ORT, Occtet Curator può essere integrato nelle pipeline di build.
Questo consente di eseguire automaticamente ORT run e importare direttamente i risultati.

Anche le esecuzioni avviate esternamente vengono rilevate e possono essere importate facilmente.

In questo modo puoi mantenere le SBOM sempre aggiornate e valutare continuamente le vulnerabilità.

## 18. Processo di audit – Revisione e curatela di una SBOM

L'importazione di una SBOM costituisce inizialmente una raccolta automatizzata dei componenti software rilevati nel progetto. Tuttavia, il risultato di un'analisi può essere incompleto: alcuni componenti potrebbero non essere rilevati dallo scanner (che ha generato la SBOM), potrebbero essere assegnati in modo errato oppure potrebbero non disporre di informazioni sufficienti relative a licenze, diritti d'autore o vulnerabilità.

L'obiettivo del processo di audit è pertanto confrontare le informazioni generate automaticamente con il contenuto effettivo del codice sorgente e completare la SBOM. L'obiettivo è creare un inventario del software il più completo, tracciabile e verificabile possibile.

La revisione viene effettuata essenzialmente seguendo i seguenti passaggi:

1. **Esaminare il progetto e il Filetree**
2. **Identificare e aggiungere i componenti software non rilevati**
3. **Esaminare i file e le indicazioni legali**
4. **Assegnare le informazioni agli InventoryItems e documentarle nelle AuditNotes**
5. **Esaminare e aggiornare le vulnerabilità dei componenti software**
6. **Concludere la revisione e contrassegnare i file come reviewed**

### 18.1 Esaminare il progetto e il Filetree

Dopo aver importato la SBOM, si esamina innanzitutto l'intero progetto in SBOM Management. Il Filetree situato a sinistra e la griglia degli InventoryItems situata a destra servono come punti di partenza per la revisione manuale.

L'obiettivo è ottenere una panoramica di quali file facciano effettivamente parte del progetto e quali di essi siano già assegnati a un InventoryItem.

In particolare, è necessario verificare i seguenti punti:

- Sono presenti tutti i file rilevanti del progetto?
- I file sono già assegnati a un InventoryItem appropriato?
- Esistono file che non sono ancora assegnati ad alcun InventoryItem?
- Esistono InventoryItems per i quali non è stato possibile scaricare alcun codice per i SoftwareComponents?
- Vi sono indicazioni che lo scanner non abbia rilevato un componente software?
- I file contengono indicazioni relative a licenze o diritti d'autore che non sono ancora state prese in considerazione nella SBOM importata?

Una revisione completa del Filetree è particolarmente importante nei progetti le cui dipendenze non possono essere identificate esclusivamente tramite il gestore dei pacchetti utilizzato o lo scanner automatico.

Tuttavia, il Filetree può rimanere incompleto se non è stato possibile scaricare il codice di un InventoryItem. Ciò può accadere, ad esempio, se l'URL di un SoftwareComponent è invalido, errato o non è compatibile con le strategie di download del Curator. Attualmente sono supportati i repository Git e Maven.

### 18.2 Identificare i componenti software non rilevati

Un audit non si limita a esaminare gli InventoryItems forniti dallo scanner/import. È inoltre necessario determinare se nel codice sorgente siano presenti componenti software che non sono stati rilevati dallo scanner.

Se, ad esempio, vengono trovati file che appartengono chiaramente a un componente di terze parti per il quale non esiste ancora alcun InventoryItem corrispondente, tale componente deve essere aggiunto manualmente a Occtet.

A tale scopo, si crea o si seleziona innanzitutto un **SoftwareComponent** appropriato e successivamente si crea il relativo **InventoryItem** per il progetto.

L'InventoryItem costituisce quindi il collegamento tra il componente software tecnico e il progetto concreto. Solo attraverso questa associazione è possibile valutare completamente il componente nell'ambito dell'audit del progetto.

Per identificare componenti sconosciuti, possono essere utili, tra gli altri, i seguenti indizi:

- Nomi di file e directory
- Strutture di pacchetti e namespace
- Intestazioni di copyright e licenza
- File README o NOTICE
- Documentazione inclusa nel componente
- Informazioni sulla versione
- Riferimenti a fornitori esterni o progetti esterni
- Strutture di file tipiche o frammenti di codice sorgente conosciuti

L'inserimento manuale è particolarmente importante, poiché una SBOM può essere completa solo quanto il processo di rilevamento su cui si basa.

### 18.3 Esaminare i file e le informazioni legali

Successivamente, i file assegnati agli InventoryItems vengono esaminati in dettaglio.

Il Code Viewer consente di aprire i file, esaminarli e cercare termini rilevanti. Oltre al codice sorgente stesso, sono particolarmente rilevanti le informazioni legali e relative alle licenze. A tale scopo, Occtet può evidenziare nel contenuto dei file i termini di ricerca configurati.

Durante la revisione è necessario cercare in particolare le seguenti informazioni:

- Intestazioni delle licenze
- SPDX-License-Identifier
- Indicazioni di copyright
- File NOTICE e altre indicazioni legali
- Riferimenti a codice di terze parti
- Informazioni sull'origine del codice
- Condizioni d'uso aggiuntive
- Restrizioni all'esportazione o altre restrizioni legali

Non è sufficiente determinare semplicemente l'esistenza di una licenza. È inoltre importante determinare **quale parte del progetto sia coperta dalla licenza corrispondente** e verificare se le informazioni trovate appartengano effettivamente al componente software in esame.

Ad esempio, un repository può contenere diversi componenti con licenze differenti. Allo stesso modo, alcuni file possono avere una propria intestazione di licenza, mentre per un componente di livello superiore viene documentata una licenza diversa.

### 18.4 Documentare le informazioni nell'InventoryItem

Le informazioni ottenute durante la revisione dei file vengono successivamente consolidate nel relativo InventoryItem.

Per ogni componente software, dovrebbe essere possibile tracciare in particolare le seguenti informazioni:

- quali file appartengono al componente,
- quale licenza è stata identificata per il componente o per i file associati,
- quali indicazioni di copyright sono presenti,
- dove sono state trovate queste informazioni nel progetto,
- quali decisioni manuali sono state prese nell'ambito dell'audit.

Le **AuditNotes** servono come prova e tracciabilità delle decisioni prese. Dovrebbero descrivere nel modo più concreto possibile **cosa è stato identificato e dove è possibile trovare la relativa evidenza**.

Esempio:

*«Licenza MIT trovata nel file LICENSE della directory /thirdparty/example. Indicazione di copyright trovata nell'intestazione di example.java. File assegnati all'InventoryItem Example Library 2.4.»*

Ciò consente di comprendere successivamente su quale base siano state inserite le informazioni relative alla licenza e al copyright.

Le AuditNotes non sono quindi semplicemente una nota personale del Curator. Forniscono la motivazione delle decisioni manuali e servono quindi come supporto per audit successivi, richieste dei clienti e revisioni legali.

### 18.5 Valutare le licenze e i copyright

Le informazioni relative a licenze e copyright trovate nei file vengono confrontate con le informazioni già identificate dallo scanner/import.

Possono verificarsi, ad esempio, le seguenti situazioni:

- La licenza rilevata automaticamente viene confermata da un'intestazione di licenza inequivocabile.
- Una licenza è stata rilevata automaticamente, ma non può essere verificata chiaramente nel codice sorgente.
- Viene trovata una licenza aggiuntiva in un file che non era stato preso in considerazione dallo scanner.
- Nel codice sorgente viene trovata un'indicazione di copyright che deve essere assegnata al componente.
- Sono presenti diverse indicazioni di licenza che richiedono una valutazione più approfondita.

In caso di informazioni contraddittorie o poco chiare, la decisione non dovrebbe essere presa senza una giustificazione. Al contrario, le informazioni su cui si basa la decisione devono essere documentate nelle AuditNotes e, se necessario, esaminate più approfonditamente.

Soprattutto nel caso dei componenti Open Source, è importante distinguere tra l'**identificazione tecnica di una licenza** e la **valutazione giuridica delle sue implicazioni**. Occtet consente di raccogliere queste informazioni e di garantirne la tracciabilità; la valutazione giuridica vera e propria può richiedere una revisione aggiuntiva in funzione del progetto e della licenza corrispondente.

### 18.6 Esaminare le vulnerabilità dei componenti software

Una volta verificati l'identità e l'assegnazione dei componenti software, vengono esaminate le loro vulnerabilità note.

Occtet assegna inizialmente automaticamente le vulnerabilità ai SoftwareComponents. Durante l'audit si verifica successivamente se tale assegnazione sia rilevante per l'utilizzo concreto del componente nel progetto.

Per ogni vulnerabilità rilevante devono essere verificati in particolare i seguenti punti:

- Il componente software interessato è effettivamente presente nel progetto?
- La versione rilevata corrisponde alla versione effettivamente utilizzata?
- La vulnerabilità è rilevante per questa versione?
- La parte vulnerabile del componente viene effettivamente utilizzata nel progetto?
- La vulnerabilità è già stata corretta o eliminata mediante una versione più recente?
- La vulnerabilità è effettivamente sfruttabile nel contesto specifico del progetto o del suo utilizzo?

I risultati di questa revisione vengono successivamente aggiornati nella vista dell'InventoryItem per le Vulnerabilities corrispondenti.

Ad esempio, possono essere utilizzati i seguenti stati:

- **Affected** – il componente è interessato dalla vulnerabilità.
- **Not Affected** – la vulnerabilità non riguarda l'utilizzo concreto del componente.
- **Under Investigation** – la valutazione non è ancora stata completata.
- **Fixed** – la vulnerabilità, ad esempio, è stata risolta mediante un aggiornamento.

Queste valutazioni possono essere successivamente esportate come informazioni VEX.

È importante distinguere tra una **vulnerabilità nota di un componente** e il **rischio effettivo per il progetto concreto**. La presenza di una CVE non significa automaticamente che l'applicazione sia sfruttabile. Per effettuare una valutazione affidabile devono essere presi in considerazione, tra gli altri fattori, la versione, la funzionalità interessata e l'utilizzo concreto del componente.

### 18.7 InventoryItem come unità centrale dell'audit

L'InventoryItem costituisce pertanto l'unità centrale dell'audit. In esso vengono raggruppate le diverse informazioni relative a un componente software:

- SoftwareComponent e versione
- File associati
- Licenze
- Copyright
- Vulnerabilità
- Valutazioni VEX
- AuditNotes

L'obiettivo è che un auditor o un altro Curator possa utilizzare l'InventoryItem per comprendere **quale componente viene utilizzato, da quali file è composto, quali informazioni legali sono disponibili e come sono stati valutati gli aspetti relativi alla sicurezza**.

### 18.8 Concludere l'audit

Un file dovrebbe essere contrassegnato come **reviewed** solo una volta che la sua assegnazione e le informazioni rilevanti sono state verificate.

Per considerare una revisione completata, è necessario garantire in particolare quanto segue:

- Il file è assegnato all'InventoryItem corretto.
- Il componente software è stato identificato in modo inequivocabile.
- Le informazioni relative alla licenza sono state esaminate.
- Le informazioni relative al copyright sono state esaminate.
- Le informazioni legali aggiuntive rilevanti sono state prese in considerazione.
- Le vulnerabilità rilevate sono state esaminate e valutate.
- Le decisioni manuali sono state documentate nelle AuditNotes.

Questo processo viene ripetuto per i file e i componenti software rilevanti del progetto.

Al termine dell'audit, la SBOM importata non dovrebbe più rappresentare semplicemente un elenco di componenti generato automaticamente, bensì un **inventario del software verificato e curato in modo tracciabile**.

Le informazioni ottenute in questo modo costituiscono successivamente la base per le valutazioni di sicurezza, le analisi delle licenze e dei copyright, nonché per la creazione di report VEX e di una SBOM perfezionata.
