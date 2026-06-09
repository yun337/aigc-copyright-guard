const { execSync } = require('child_process');
const path = require('path');

// Run hardhat deploy via the local installation
const contractsDir = __dirname;
const hardhatBin = path.join(contractsDir, 'node_modules', '.bin', 'hardhat');

try {
  const result = execSync(
    `"${process.execPath}" "${hardhatBin}" run scripts/deploy.js --network localhost`,
    { cwd: contractsDir, stdio: 'pipe', timeout: 60000 }
  );
  console.log(result.stdout?.toString() || '');
  console.error(result.stderr?.toString() || '');
  console.log('DEPLOY SUCCESS');
} catch (e) {
  console.log('STDOUT:', e.stdout?.toString() || '');
  console.log('STDERR:', e.stderr?.toString() || '');
  console.log('ERROR:', e.message);
}
