const argv = require('minimist')(process.argv.slice(2));

const recordsCnt = argv['recordsCnt'];
if (!recordsCnt)
  throw new Error('Please specify the number of records that will be generated: --recordsCnt');

for(let i = 0; i < recordsCnt; i++) {
  console.log(`${vrn()},${randomReason()},${randomManufacturer()},C`);
}

function vrn() {
  return `${randomUppercaseString(2)}${randomDigit()}${randomDigit()}${randomUppercaseString(3)}`
}

function randomDateGreaterThan(start) {
  const end = Math.random()*24*60*60*1000 * 3*30;
  const date = new Date(start.getTime() + end);
  return date;
}

function toISODate(input) {
  return input.toISOString().substring(0, 10);
}

function randomReason() {
  const categories = ["reason 1", "reason 2", "reason 3", "reason 5"];
  return categories[ randomIntInclusive(0, categories.length - 1)  ];
}

function randomManufacturer() {
  const models = ["manufaturer 1", "manufaturer 2", "manufaturer 3", "manufaturer 4"];
  return models[ randomIntInclusive(0, models.length - 1)  ];
}

function randomUppercaseString(length) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  var result = '';
  for (var i = length; i > 0; --i) result += chars[Math.floor(Math.random() * chars.length)];
  return result;
}

function randomDigit() {
  return Math.floor(Math.random() * 10) ;
}

function randomIntInclusive(min, max) {
  min = Math.ceil(min);
  max = Math.floor(max);
  return Math.floor(Math.random() * (max - min + 1)) + min; //The maximum is inclusive and the minimum is inclusive 
}

