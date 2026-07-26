# OpenAstroAPI

OpenAstroAPI is a Java/Spring Boot API for astrological calculations.

## Configuration

Set the API signing secret before starting the application:

```bash
export ASTRO_API_SECRET_KEY="replace-with-a-secure-random-value"
```

The token lifetime can optionally be configured in seconds:

```bash
export ASTRO_API_TOKEN_EXPIRATION="3600"
```

## Build

```bash
mvn test
mvn package
```

## License

OpenAstroAPI is distributed under the GNU Affero General Public License,
version 3 or any later version (`AGPL-3.0-or-later`). See `LICENSE`.

This project includes the Swiss Ephemeris Java port and Swiss Ephemeris data
files. Their copyright and licensing notices are documented in
`THIRD_PARTY_NOTICES.md`. Anyone deploying a modified version as a network
service must also observe the corresponding source-code offer obligations.
The matching, unmodified source archive for the bundled Swiss Ephemeris JAR is
included under `third_party/swisseph/`.
