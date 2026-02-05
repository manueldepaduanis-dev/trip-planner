# Itinerary manager with intelligent queue management

__Benvenuti nel sistema di gestione itinerari più mvp di sempre. Questo non è il solito progetto ultra ingegnerizzato che richiede un data center della NASA per essere avviato. È un MVP costruito per essere veloce, portable e "ready-to-break".__

***

### Stack tecnologico:
- Java 21 LTS
- Spring boot 4.0.2 LTS
- H2 in memory DB

***

Per questo MVP di gestione itinerari ho deciso di utilizzare tecnologie easy to use, in quanto voglio garantire la portabilità di questo progetto. Mi sono affidato ad una gestione della coda in-memory (ThreadPoolTaskExecutor, LinkedBlocking Queue), assumendomi i rischi di perdere i task in coda qualora questo MVP si stoppasse, in quanto ho ritenuto over-enginereed utilizzare dei message broker esterni (come RabbitMQ) o Redis, sia perchè il traffico non è tale da giustificarlo e anche perchè un MVP dovrebbe essere appunto portable. Ma ciò non ci vieta di implementare questa logica nei cicli di iterazione futuri.

Per quanto riguarda il requisito **__user as guest__**, ho deciso di staccare un session_id (qualora non fornito nell'header) in risposta alla creazione di un nuovo itinerario (POST), con il quale quell'utente non loggato può visualizzare o gestire la lista degli itinerari creati da lui in quella sessione. Fin quando quel session_id è in mano all'utente, i suoi itinerari sono "sani e salvi".

Questa infrastruttura prevede fino a 5 task eseguiti in parallelo, con altri 3 pronti a dare una mano ai fratelli maggiori :D,  ed una coda limitata a 100 posti solo per far vedere che l'implementazione funziona; Ovviamente la coda DEVE essere limitata a mio avviso per una questione di gestione della __backpressure__, a meno che non abbiamo disponibiltà lato server molto ingenti :D.

Per poter testare la gestione coda ho scritto un file bash da lanciare, il quale lancia 200 richieste in simultanea. Il file si chiama __"break_my_api_if_you_can.sh"__.

***

### Endpoints:

| Metodo | Endpoint | Descrizione | Risposta |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/itineraries` | Crea un nuovo itinerario. Entra in coda se il pool è pieno. | `201 Created` / `429 Too Many Requests` |
| **GET** | `/api/v1/itineraries/{id}` | Recupera i dettagli e lo stato (`IN_QUEUE`, `PROCESSING`, `COMPLETED`). | `200 OK` / `404 Not Found` |
| **PUT** | `/api/v1/itineraries/{id}` | Aggiorna la tappa attuale o modifica l'itinerario (anche se in coda). | `200 OK` / `400 Bad Request` |
| **GET** | `/api/v1/itineraries` | Visualizza una lista di itinerari con annesso status. | `200 OK` |
| **GET** | `/api/v1/geo` | Endpoint mock per dati geografici (`Country` -> `City`). | `200 OK` |

***

##### Buon test!
