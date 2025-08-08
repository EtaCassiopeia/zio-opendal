# Use OpenJDK 17 on Linux AMD64 for better compatibility
FROM openjdk:17-jdk-slim

# Install SBT and other build dependencies
RUN apt-get update && apt-get install -y \
    curl \
    gnupg \
    apt-transport-https \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Add SBT repository and install SBT
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add -
RUN apt-get update && apt-get install -y sbt=1.11.4 && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Set environment variables for native access and ZIO OpenDAL
ENV JAVA_OPTS="--enable-native-access=ALL-UNNAMED -Djna.nosys=true"
ENV SBT_OPTS="-Xmx3G -Xms1G"

# Default command
CMD ["sbt"]
