import { readAcademicCsv } from './import-csv';
describe('Academic CSV import', () => {
  const header = 'studentId,programId,registrationCode,enrolledOn\r\n';
  it('reads UTF-8 BOM, quoted commas and escaped quotes', () => {
    const rows = readAcademicCsv('\uFEFF' + header + 'student,program,"MAT,""01""",2026-01-01\r\n');
    expect(rows[0].registrationCode).toBe('MAT,"01"');
  });
  it('rejects incomplete rows, invalid headers and unfinished quotes', () => {
    for (const text of [
      'name,email\nA,B',
      header + 'student,program,code',
      header + 'student,program,"code,2026-01-01',
    ])
      expect(() => readAcademicCsv(text)).toThrow();
  });
  it('accepts 100 entries and rejects larger batches', () => {
    const row = 'student,program,code,2026-01-01\n';
    expect(readAcademicCsv(header + row.repeat(100)).length).toBe(100);
    expect(() => readAcademicCsv(header + row.repeat(101))).toThrow();
  });
});
