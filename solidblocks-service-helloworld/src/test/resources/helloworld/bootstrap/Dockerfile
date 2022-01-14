FROM nginx

ARG SOLIDBLOCKS_GREEN_VERSION
ARG SOLIDBLOCKS_BLUE_VERSION

ARG GITHUB_USERNAME=pellepelster

ARG BLUE_HTML_DIR="/etc/nginx/html/${GITHUB_USERNAME}/solidblocks/solidblocks/solidblocks-service-helloworld/${SOLIDBLOCKS_BLUE_VERSION}"
ARG GREEN_HTML_DIR="/etc/nginx/html/${GITHUB_USERNAME}/solidblocks/solidblocks/solidblocks-service-helloworld/${SOLIDBLOCKS_GREEN_VERSION}"

COPY nginx.conf /etc/nginx/nginx.conf
COPY .htpasswd /etc/nginx/.htpasswd

RUN mkdir -p ${BLUE_HTML_DIR}
RUN mkdir -p ${GREEN_HTML_DIR}

COPY "artefacts/solidblocks-service-helloworld-${SOLIDBLOCKS_BLUE_VERSION}.tar" "${BLUE_HTML_DIR}/solidblocks-service-helloworld-${SOLIDBLOCKS_BLUE_VERSION}.tar"
COPY "artefacts/solidblocks-service-helloworld-${SOLIDBLOCKS_GREEN_VERSION}.tar" "${GREEN_HTML_DIR}/solidblocks-service-helloworld-${SOLIDBLOCKS_GREEN_VERSION}.tar"
