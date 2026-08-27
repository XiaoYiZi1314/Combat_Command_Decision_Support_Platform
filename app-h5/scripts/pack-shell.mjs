import { cpSync, existsSync, mkdirSync, rmSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const dist = join(root, 'dist');
const target = process.argv[2] === 'harmony' ? 'harmony' : 'android';
const dest = target === 'harmony'
  ? join(root, 'harmony-shell', 'entry', 'src', 'main', 'resources', 'rawfile', 'www')
  : join(root, 'android-shell', 'app', 'src', 'main', 'assets', 'www');

if (!existsSync(dist) || !existsSync(join(dist, 'index.html'))) {
  throw new Error('先执行 npm run build，再 pack:' + target);
}

rmSync(dest, { recursive: true, force: true });
mkdirSync(dest, { recursive: true });
cpSync(dist, dest, { recursive: true });
console.log('packed', target, '->', dest);
