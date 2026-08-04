$ErrorActionPreference = "Stop"

$images = @(
    "distributed-tinyurl-url-service",
    "distributed-tinyurl-auth-service",
    "distributed-tinyurl-analytics-service"
)

foreach ($image in $images) {
    $registryImage = "localhost:5000/${image}:latest"
    $archivePath = "C:\tmp\${image}.tar"
    $nodeArchivePath = "/${image}.tar"

    docker tag "${image}:latest" $registryImage
    docker save $registryImage -o $archivePath
    docker cp $archivePath "desktop-control-plane:$nodeArchivePath"
    docker exec desktop-control-plane bash -lc "ctr -n k8s.io images import $nodeArchivePath"
}

docker exec desktop-control-plane bash -lc "ctr -n k8s.io images ls | grep distributed-tinyurl"
