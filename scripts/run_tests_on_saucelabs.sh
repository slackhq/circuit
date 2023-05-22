#!/bin/bash
set -exo pipefail


echo "[ INFO ] Downloading Sauce Labs Executable"
sh -c 'curl -L https://saucelabs.github.io/saucectl/install | bash -s -- -b ./'
chmod +x saucectl

echo "[ INFO ] Executing Tests on Sauce Labs"
saucectl configure --username sso-saleforce-j.stewart --accessKey ${SAUCELABS_TOKEN}
saucectl run -c scripts/sauce_labs_config.yml