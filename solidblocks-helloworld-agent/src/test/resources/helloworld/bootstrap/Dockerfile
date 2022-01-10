FROM nginx

ARG SOLIDBLOCKS_GREEN_VERSION
ARG SOLIDBLOCKS_BLUE_VERSION

ARG GITHUB_USERNAME=pellepelster

ARG BLUE_HTML_DIR="/etc/nginx/html/${GITHUB_USERNAME}/solidblocks/solidblocks/solidblocks-helloworld-agent/${SOLIDBLOCKS_BLUE_VERSION}"
ARG GREEN_HTML_DIR="/etc/nginx/html/${GITHUB_USERNAME}/solidblocks/solidblocks/solidblocks-helloworld-agent/${SOLIDBLOCKS_GREEN_VERSION}"

COPY nginx.conf /etc/nginx/nginx.conf
COPY .htpasswd /etc/nginx/.htpasswd

RUN mkdir -p ${BLUE_HTML_DIR}
RUN mkdir -p ${GREEN_HTML_DIR}

#"https://maven.pkg.github.com/${GITHUB_USERNAME}/solidblocks/solidblocks/solidblocks-cloud-init/${SOLIDBLOCKS_VERSION}/solidblocks-cloud-init-${SOLIDBLOCKS_VERSION}.jar" > "${temp_file}"

COPY "artefacts/solidblocks-helloworld-agent-${SOLIDBLOCKS_BLUE_VERSION}.tar" "${BLUE_HTML_DIR}/solidblocks-helloworld-agent-${SOLIDBLOCKS_BLUE_VERSION}.tar"
COPY "artefacts/solidblocks-helloworld-agent-${SOLIDBLOCKS_GREEN_VERSION}.tar" "${GREEN_HTML_DIR}/solidblocks-helloworld-agent-${SOLIDBLOCKS_GREEN_VERSION}.tar"
