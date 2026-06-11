import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

export const resources = {
  mk: {
    translation: {
      appName: 'CarCare ASMS',
      login: 'Најава',
      email: 'Е-пошта',
      password: 'Лозинка',
      changePassword: 'Промена на лозинка',
      dashboard: 'Контролна табла',
      customers: 'Клиенти',
      vehicles: 'Возила',
      appointments: 'Термини',
      services: 'Сервисна историја',
      offers: 'Понуди',
      documents: 'Документи',
      admin: 'Администрација',
      save: 'Зачувај',
      send: 'Испрати',
      cancel: 'Откажи',
      create: 'Креирај',
      edit: 'Уреди',
      details: 'Детали',
      loading: 'Се вчитува...',
      noData: 'Нема податоци за приказ.',
      logout: 'Одјава',
      bookAppointment: 'Закажи термин',
      newService: 'Нов сервис',
      createOffer: 'Креирај понуда',
      pdfPreview: 'PDF прегледот ќе се прикаже тука кога документот ќе биде достапен.',
      employees: 'Вработени',
      loyalty: 'Лојалност',
      required: 'Полето е задолжително',
      invalidEmail: 'Внесете валидна е-пошта',
      saved: 'Успешно зачувано',
      updated: 'Успешно ажурирано',
      rescheduleFailed: 'Преместувањето не успеа. Терминот е вратен назад.'
    }
  },
  en: {
    translation: {
      appName: 'CarCare ASMS',
      login: 'Login',
      email: 'Email',
      password: 'Password',
      changePassword: 'Change password',
      dashboard: 'Dashboard',
      customers: 'Customers',
      vehicles: 'Vehicles',
      appointments: 'Appointments',
      services: 'Service history',
      offers: 'Offers',
      documents: 'Documents',
      admin: 'Admin',
      save: 'Save',
      send: 'Send',
      cancel: 'Cancel',
      create: 'Create',
      edit: 'Edit',
      details: 'Details',
      loading: 'Loading...',
      noData: 'No data to display.',
      logout: 'Logout',
      bookAppointment: 'Book appointment',
      newService: 'New service',
      createOffer: 'Create offer',
      pdfPreview: 'PDF preview will appear here when the document is available.',
      employees: 'Employees',
      loyalty: 'Loyalty',
      required: 'This field is required',
      invalidEmail: 'Enter a valid email',
      saved: 'Saved successfully',
      updated: 'Updated successfully',
      rescheduleFailed: 'Reschedule failed. Appointment was rolled back.'
    }
  }
} as const;

i18n.use(initReactI18next).init({
  resources,
  lng: 'mk',
  fallbackLng: 'en',
  interpolation: { escapeValue: false }
});

export default i18n;
