const { ClassicLevel } = require('classic-level');
const recs = require('./records.json');
(async () => {
  const db = new ClassicLevel(process.argv[2], { keyEncoding: 'buffer', valueEncoding: 'buffer', compression: false });
  await db.open();
  const batch = Object.entries(recs).map(([k, v]) => ({ type: 'put', key: Buffer.from(k, 'hex'), value: Buffer.from(v, 'hex') }));
  await db.batch(batch);
  await db.compactRange(Buffer.alloc(0), Buffer.from('ffffffffffffffff', 'hex'));
  await db.close();
})();
