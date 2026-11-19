const S = require('snappyjs');
const inputs = {
  empty: Buffer.alloc(0),
  hello: Buffer.from('hello'),
  repeat: Buffer.from('abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc'),
  pattern: Buffer.from(Array.from({length: 3000}, (_, i) => (i % 251) ^ ((i >> 7) & 0x0f))),
  text: Buffer.from('The quick brown fox jumps over the lazy dog. '.repeat(40) + 'Tectonicus bedrock leveldb reader!'.repeat(3)),
  run: Buffer.alloc(70000, 0x61),
};
for (const [k, v] of Object.entries(inputs)) {
  console.log(k, v.length, Buffer.from(S.compress(v)).toString('hex'));
}
