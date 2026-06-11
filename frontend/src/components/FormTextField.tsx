import { TextField, type TextFieldProps } from '@mui/material';
import { Controller, type Control, type FieldValues, type Path } from 'react-hook-form';

interface Props<T extends FieldValues> extends Omit<TextFieldProps, 'name'> {
  control: Control<T>;
  name: Path<T>;
}

export function FormTextField<T extends FieldValues>({ control, name, ...props }: Props<T>) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => (
        <TextField {...field} {...props} error={Boolean(fieldState.error)} helperText={fieldState.error?.message ?? props.helperText} />
      )}
    />
  );
}
