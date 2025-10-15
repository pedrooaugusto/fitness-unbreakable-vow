// generate-web3j-clients.js
const fs = require("fs");
const path = require("path");
const { execSync, spawnSync } = require("child_process");

// === CONFIG ===
const contracts = [
  {
    name: "FitnessUnbreakableVow",
    source: path.join(
      __dirname,
      "..",
      "artifacts/contracts/FitnessUnbreakableVow.sol/FitnessUnbreakableVow.json"
    ),
  },
  {
    name: "PhysicalActivityOracle",
    source: path.join(
      __dirname,
      "..",
      "artifacts/contracts/PhysicalActivityOracle.sol/PhysicalActivityOracle.json"
    ),
  },
];

const resourcesDir = path.resolve(
  __dirname,
  "..",
  "@androidapp/app/src/main/resources/contracts"
);
const javaOutputDir = path.resolve(
  __dirname,
  "..",
  "@androidapp/app/src/main/java"
);
const javaPackage = "com.august.fitnessvowsync.contract";
// Windows only
const web3jPath = `~/Downloads/web3j-1.7.0/web3j-1.7.0/bin/web3j`;

// === SCRIPT ===
if (!fs.existsSync(resourcesDir)) {
  fs.mkdirSync(resourcesDir, { recursive: true });
}

contracts.forEach((contract) => {
  console.log(`Processing ${contract.name}...`);

  const json = JSON.parse(fs.readFileSync(contract.source, "utf8"));

  // Write ABI file
  const abiPath = path.join(resourcesDir, `${contract.name}.abi`);
  fs.writeFileSync(abiPath, JSON.stringify(json.abi, null, 2));
  console.log(`  ABI written to ${abiPath}`);

  // Write BIN file
  const binPath = path.join(resourcesDir, `${contract.name}.bin`);
  fs.writeFileSync(binPath, json.bytecode.replace(/^0x/, ""));
  console.log(`  BIN written to ${binPath}`);

  // Run web3j code generation
  const cmd = `${web3jPath} generate solidity -a ${abiPath} -b ${binPath} -o ${javaOutputDir} -p ${javaPackage}`.replaceAll('\\', '/');

  console.log(`  Running web3j for ${contract.name}...`);

  execSync(cmd, { stdio: "inherit", shell: "C:\\Program Files\\Git\\bin\\bash.exe" });

  // Fix an error with web3j where the custom error class is not defined...
  const clientPath = path.resolve(javaOutputDir, ...javaPackage.split('.'), `${contract.name}.java`)
  const clientContentLines = fs.readFileSync(clientPath, { encoding: 'utf-8' }).split('\n');

  for (let i = 0; i < clientContentLines.length; i++) {
    if (clientContentLines[i] === 'import org.web3j.abi.datatypes.CustomError;') {
      clientContentLines[i] = '';
      break;
    }
  }

  const classDefinitionLine = clientContentLines.findIndex(line => line === `public class ${contract.name} extends Contract {`)

  clientContentLines[classDefinitionLine] += '\n    public static class CustomError { public CustomError(Object a, Object b){} }'

  fs.writeFileSync(clientPath, clientContentLines.join('\n'));
});

console.log("✅ All contracts processed successfully.");
