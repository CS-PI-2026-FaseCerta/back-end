docker build -t fasecerta .

docker rm -f fasecerta 2>$null

docker run -d `
  --name fasecerta `
  -p 8080:8080 `
  --env-file .env `
  -e DB_HOST=host.docker.internal `
  fasecerta