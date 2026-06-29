import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';
import { apiFieldErrors } from '../components/ApiErrorAlert';

export const applyApiFieldErrors = <T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fieldMap: Partial<Record<string, Path<T>>> = {}
) => {
  const fieldErrors = apiFieldErrors(error);
  Object.entries(fieldErrors).forEach(([field, message]) => {
    setError((fieldMap[field] ?? field) as Path<T>, { type: 'server', message });
  });
  return fieldErrors;
};
