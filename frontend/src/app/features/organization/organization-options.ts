export const DOCUMENT_TYPES = [
  { value: 'CI', label: 'CI — Cédula de identidad' },
  { value: 'DNI', label: 'DNI — Documento nacional de identidad' },
  { value: 'PASAPORTE', label: 'Pasaporte' },
  { value: 'CE', label: 'Cédula o carné de extranjería' },
  { value: 'OTRO', label: 'Otro documento de identidad' },
];

export const REPRESENTATIVE_POSITIONS = [
  'Director/a', 'Rector/a', 'Administrador/a', 'Representante legal',
  'Coordinador/a', 'Encargado/a', 'Otro cargo',
].map(label => ({ value: label, label }));

export const BOLIVIA_DEPARTMENTS = [
  'Beni', 'Chuquisaca', 'Cochabamba', 'La Paz', 'Oruro',
  'Pando', 'Potosí', 'Santa Cruz', 'Tarija',
].map(label => ({ value: label, label }));

// Store two-letter codes in the existing API contract; display localized names.
const REGION_CODES = `AD AE AF AG AI AL AM AO AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR BS BT BW BY BZ
CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ EC EE EG ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GT GU GW GY
HK HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MN MO MP MQ MR MS MT MU MV MW MX MY MZ
NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RE RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ
TC TD TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS XK YE YT ZA ZM ZW`;
const regionNames = new Intl.DisplayNames(['es'], { type: 'region' });
export const COUNTRY_OPTIONS = REGION_CODES.split(/\s+/)
  .map(value => ({ value, label: regionNames.of(value) ?? value }))
  .sort((a, b) => a.label.localeCompare(b.label, 'es'));
