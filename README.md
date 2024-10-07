# Keycloak events Falco exporter

To use this exporter, you also have to configure `Falco` by installing and configuring the [Falco
`keycloak` plugin](https://github.com/mattiaforc/falco-keycloak-plugin).

## Keycloak compatibility

Each release workflow runs and builds the plugin against multiple keycloak versions to ensure compatibility.
This is the compatibility matrix:

| plugin \ keycloak | v21 | v22 | v23 | v24 | v25 | v26 |
|-------------------|-----|-----|-----|-----|-----|-----|
| 0.1.0             | ✅  | ✅  | ✅  | ✅  | ❌  |  ❌ |
| 0.1.1             | ✅  | ✅  | ✅  | ✅  | ✅  |  ❌ |
| 0.2.0             | ✅  | ✅  | ✅  | ✅  | ✅  |  ✅ |

## Installation

Download [the plugin](https://github.com/mattiaforc/keycloak-events-falco-exporter/releases) (*preferred*)
or [build locally](#development).

Copy the `spi-falco-event-<version>.jar` into `$KEYCLOAK_HOME/providers` folder.
If you're already pre-building and optimizing keycloak, once placed the `.jar` in that folder you would run:

```bash
/opt/keycloak/bin/kc.sh build
```

and then you can start keycloak with

```bash
/opt/keycloak/bin/kc.sh start --optimized
```

Otherwise, you can simply run `start` and Keycloak will configure the new plugin at start time.

You can see a working example of a Dockerfile keycloak customization (`./Dockerfile`) that will first build the plugin
(without needing Maven/Java installed), then from a keycloak base image it will launch the keycloak build script to add
the Falco provider.

Once you have Keycloak running with the exporter installed and [configured](#configuration), you can enable the Falco event listener for your realm by going into the Realm settings, under the tab Events you will now be able to select `falco`, amongst other event listeners.

## Configuration

Properties can be set via environment variables (*preferred*, for example `FALCO_ENDPOINT`) or as parameters when
starting keycloak (for example `--spi-events-listener-falco-falcoEndpoint`).

- `falcoEndpoint` (`FALCO_ENDPOINT`): Mandatory configuration for the Falco host that will receive the events. This
  could be both HTTP or HTTPS. Its configuration is left to user discretion, see the README for more details on the
  plugin architecture on the Falco side.
- *Optional*: `falcoEvents` (`FALCO_EVENTS`): A comma-separated list of all keycloak user events to forward to Falco. If
  this is empty, then we forward every type of event.
- *Optional*: `falcoAdminEvents` (`FALCO_ADMIN_EVENTS`): A boolean (`true` or `false` as string) for enabling forwarding
  of keycloak admin events to Falco.
- *Optional*: `falcoAdditionalHeaders` (`FALCO_ADDITIONAL_HEADERS`): Additional headers to add for every HTTP call this
  exporter makes to Falco, for example if Falco is behind an API Gateway or some sort of additional HTTP
  authentication/validation.
  Headers are separated with semicolons (`;`), with the format `HEADER_KEY=HEADER_VALUE`.
  For example,
  suppose we want to add the header `X-Api-Key` with value `foo` and the header `My-Header` with value `bar`,
  the configuration string would be: `X-Api-Key=foo;My-Header=bar`.

You can see all the keycloak events [here](https://www.keycloak.org/docs/latest/server_admin/#event-types)
(note that this link points to the latest keycloak version. Be sure to check the documentation for your keycloak
version) and in
the [keycloak source code](https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/EventType.java).

## Development

Prerequisites:

- Docker (Compose)
- *Optional*: JDK 17+
- *Optional*: Maven

To build the plugin locally, run:

```bash
mvn clean install
```

The resulting `.jar` plugin will be inside `target/spi-falco-event-<version>.jar`.

The easiest way to test and develop this plugin consists in:

- Cloning this repository locally
- *Optional*: Cloning the Falco plugin repository (if you intend to modify the Falco plugin itself – if you only need to
  alter keycloak behavior, you can omit this step).
- Downloading or building locally `libkeycloak.so` Falco plugin to be mounted on keycloak Docker container (see
  `docker-compose.yml`).
  See the [Falco plugin repository](https://github.com/mattiaforc/falco-keycloak-plugin) for more
  details or to download the plugin.
  For example, with `falcoctl` (you can see also how to use that inside the `docker-compose.yml` file): 
  ```bash
  falcoctl index add keycloak https://raw.githubusercontent.com/mattiaforc/falco-keycloak-plugin/main/index.yaml
  falcoctl artifact install keycloak
  ```
- Run `docker-compose up -d --build` that will:
    - Build the keycloak plugin and a keycloak docker image with the plugin bundled
    - Run Keycloak and Falco, along with Falco Sidekick (with UI) and its dependencies locally with docker containers.

At this point you can access Keycloak on [http://localhost:8080](http://localhost:8080), and Falco sidekick ui
on [http://localhost:2802](http://localhost:2802).
You can check the `hack` folder for Falco/Keycloak configuration and default rules.
Keycloak comes shipped with a `test` realm where Falco events are enabled by default,
so for example, creating a new user under that realm would trigger an event that would be forwarded to Falco.

If you simply want to test this extension without needing to configure/start Falco,
then you can run `docker-compose --profile extra up -d` that will also run an `echo-server` container on port `7080`.
Change the `FALCO_ENDPOINT` variable in the `keycloak` container inside
`docker-compose.yml` to point to `echo-server:7080`
and you will see every forwarded event HTTP request logged inside the `echo-server` container.
In that case, you can ignore all the Falco containers.

## Contributing

PRs and issues are very welcome, feel free to open them or reach out to me directly.
