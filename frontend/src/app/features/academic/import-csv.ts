export interface ImportRow {
  studentId: string;
  programId: string;
  registrationCode: string;
  enrolledOn: string;
}
export const IMPORT_HEADERS = ['studentId', 'programId', 'registrationCode', 'enrolledOn'];
/** Small RFC 4180 reader: quoted delimiters, escaped quotes, CRLF and quoted newlines. */
export function readAcademicCsv(text: string): ImportRow[] {
  if (text.length > 131072 || text.includes('\0'))
    throw new Error('El CSV debe tener hasta 128 KiB y contener texto válido.');
  text = text.replace(/^\uFEFF/, '');
  const records: string[][] = [];
  let row: string[] = [];
  let field = '';
  let quoted = false;
  let closed = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"') {
        if (text[i + 1] === '"') {
          field += '"';
          i++;
        } else {
          quoted = false;
          closed = true;
        }
      } else field += c;
      continue;
    }
    if (c === '"') {
      if (field || closed) throw new Error('Comillas no válidas en el CSV.');
      quoted = true;
    } else if (c === ',') {
      row.push(field);
      field = '';
      closed = false;
    } else if (c === '\r' || c === '\n') {
      if (c === '\r' && text[i + 1] === '\n') i++;
      row.push(field);
      if (row.some((x) => x.trim())) records.push(row);
      row = [];
      field = '';
      closed = false;
    } else {
      if (closed) throw new Error('Hay caracteres después del cierre de comillas.');
      field += c;
    }
    if (records.length > 101) throw new Error('Máximo 100 matrículas por lote.');
  }
  if (quoted) throw new Error('Hay un campo con comillas sin cerrar.');
  row.push(field);
  if (row.some((x) => x.trim())) records.push(row);
  if (records.length < 2 || records.length > 101)
    throw new Error('Incluye una cabecera y entre 1 y 100 matrículas.');
  if (records[0].map((x) => x.trim()).join(',') !== IMPORT_HEADERS.join(','))
    throw new Error('La cabecera debe ser: ' + IMPORT_HEADERS.join(','));
  return records.slice(1).map((values, index) => {
    if (values.length !== 4 || values.some((v) => !v.trim()))
      throw new Error('Revisa las cuatro columnas de la fila ' + (index + 2) + '.');
    return {
      studentId: values[0].trim(),
      programId: values[1].trim(),
      registrationCode: values[2].trim(),
      enrolledOn: values[3].trim(),
    };
  });
}
