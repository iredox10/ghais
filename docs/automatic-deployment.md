# Deployment and automation

The target is Heroku with PostgreSQL using the Node buildpack and the root `Procfile`. The release phase applies the schema before the web process starts. See [Heroku deployment](heroku-deployment.md) for the recorded deployment and remaining cutover work.

`.github/workflows/backend-ci.yml` builds and tests the backend and runs HTTP/database smoke checks against PostgreSQL on the GitHub runner. `.github/workflows/charts-rollup.yml` calls the Heroku API daily using the `HEROKU_ADMIN_TOKEN` repository secret and optional `HEROKU_API_URL` variable. Scheduled GitHub workflows run from the default branch.

The notes record that automatic GitHub-to-Heroku deployment has not been configured. Publishing commits and configuring any automatic deployment require valid GitHub and Heroku access. Verify the target app, branch, environment, and database before enabling deployments.
