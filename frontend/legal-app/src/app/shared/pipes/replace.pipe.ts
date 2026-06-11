import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'replace' })
export class ReplacePipe implements PipeTransform {
  transform(value: string | undefined | null, searchValue: string, replaceValue: string): string {
    if (!value) return '';
    return value.split(searchValue).join(replaceValue);
  }
}
