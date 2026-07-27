# Maven / Gradle Repository Adapter

The omnidepot repository serves as an enterprise Maven layout repository hosting JARs, POMs, and automatically synthesizing missing checksums (`.sha1`, `.sha256`, `.md5`).

---

## Configuring Maven `settings.xml`

Add omnidepot to your `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>omnidepot</id>
      <username>devuser</username>
      <password>devpassword</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>omnidepot-profile</id>
      <repositories>
        <repository>
          <id>omnidepot</id>
          <url>http://localhost:8080/maven/releases/</url>
        </repository>
      </repositories>
    </profile>
  </profiles>
</settings>
```

---

## Deploying Artifacts with Maven

Deploy artifacts directly to the repository:

```bash
mvn deploy:deploy-file \
  -DgroupId=io.omnidepot.example \
  -DartifactId=demo-sdk \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -Dfile=target/demo-sdk-1.0.0.jar \
  -DrepositoryId=omnidepot \
  -Durl=http://localhost:8080/maven/releases/
```
