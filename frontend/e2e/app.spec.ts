import { expect, test, type Page } from '@playwright/test';

const employee = {
  id: '2',
  email: 'tech@carcare.test',
  fullName: 'Integration Technician',
  enabled: true,
  failedLoginAttempts: 0,
  roles: ['ROLE_EMPLOYEE']
};

const admin = {
  id: '1',
  email: 'admin@carcare.test',
  fullName: 'Admin User',
  enabled: true,
  failedLoginAttempts: 0,
  roles: ['ROLE_ADMIN']
};

const ok = (data: unknown, message = 'OK') => ({
  success: true,
  message,
  data
});

const fail = (message: string) => ({
  success: false,
  message,
  data: null
});

async function mockBackend(page: Page) {
  await page.route('**/api/auth/login', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ accessToken: 'access-token', refreshToken: 'refresh-token', user: admin }))
    });
  });

  await page.route('**/api/auth/logout', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null, 'Logged out')) });
  });

  await page.route('**/api/auth/change-password', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null, 'Password changed')) });
  });

  await page.route('**/api/dashboard/summary', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ customers: 2, vehicles: 1, appointments: 1, serviceRecords: 1, offers: 1 }))
    });
  });

  await page.route('**/api/customers', async (route) => {
    if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '999', ...body }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '101', firstName: 'Ada', lastName: 'Lovelace', fullName: 'Ada Lovelace', email: 'ada@carcare.test', phone: '+38970111111', address: 'Analytical Lane' }]))
    });
  });

  await page.route('**/api/customers/*', async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    if (pathname.endsWith('/vehicles')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '201', customerId: '101', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123' }]))
      });
      return;
    }
    if (pathname.endsWith('/service-history')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '201', serviceDate: '2026-06-12', serviceType: 'Oil and filters', totalAmount: 3500, odometer: 123456 }]))
      });
      return;
    }
    if (request.method() === 'DELETE') {
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null, 'Deleted')) });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '101', firstName: 'Ada', lastName: 'Lovelace', fullName: 'Ada Lovelace', email: 'ada@carcare.test', phone: '+38970111111', address: 'Analytical Lane' }))
    });
  });

  await page.route('**/api/vehicles', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '201', customerId: '101', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123' }]))
    });
  });

  await page.route('**/api/vehicles/*', async (route) => {
    const pathname = new URL(route.request().url()).pathname;
    if (pathname.endsWith('/service-history')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '201', serviceDate: '2026-06-12', serviceType: 'Oil and filters', partsCost: 1500, laborCost: 2000, totalAmount: 3500, odometer: 123456, replacedParts: 'Oil filter' }]))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '201', customerId: '101', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123' }))
    });
  });

  await page.route('**/api/appointments', async (route) => {
    if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '302', customerId: body.customerId, vehicleId: body.vehicleId, scheduledAt: body.startsAt, endsAt: body.endsAt, serviceType: body.title, status: 'SCHEDULED', cancellationUrl: '/api/appointments/cancel/token-302' }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '301', customerId: '101', vehicleId: '201', scheduledAt: '2026-06-15T09:00:00.000Z', endsAt: '2026-06-15T10:00:00.000Z', serviceType: 'Oil change', status: 'SCHEDULED', cancellationUrl: '/api/appointments/cancel/token-301' }]))
    });
  });

  await page.route('**/api/appointments/available**', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ startsAt: '2026-06-20T09:00:00.000+02:00', endsAt: '2026-06-20T10:00:00.000+02:00' }]))
    });
  });

  await page.route('**/api/appointments/reminders**', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok({ sent: 1 })) });
  });

  await page.route('**/api/service-records', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '201', serviceDate: '2026-06-12', serviceType: 'Oil and filters', partsCost: 1500, laborCost: 2000, totalAmount: 3500, odometer: 123456, replacedParts: 'Oil filter' }]))
    });
  });

  await page.route('**/api/offers', async (route) => {
    if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '502', ...body, amount: Number(body.partsCost) + Number(body.laborCost), status: 'DRAFT' }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '501', customerId: '101', vehicleId: '201', title: 'Brake inspection', partsCost: 700, laborCost: 500, amount: 1200, status: 'DRAFT' }]))
    });
  });

  await page.route('**/api/offers/**', async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    if (pathname.endsWith('/pdf')) {
      await route.fulfill({ contentType: 'application/pdf', body: '%PDF-1.4 quote' });
      return;
    }
    if (pathname.endsWith('/send')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '502', customerId: '101', vehicleId: '201', title: 'Brake inspection', partsCost: 700, laborCost: 500, amount: 1200, status: 'SENT' }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '502', customerId: '101', vehicleId: '201', title: 'Brake inspection', partsCost: 700, laborCost: 500, amount: 1200, status: 'DRAFT' }))
    });
  });

  await page.route('**/api/documents', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '601', customerId: '101', serviceRecordId: '401', type: 'INSPECTION', fileName: 'Inspection report.pdf', contentType: 'application/pdf', storageKey: 'generated/inspection-report.pdf' }]))
    });
  });

  await page.route('**/api/documents/**', async (route) => {
    const pathname = new URL(route.request().url()).pathname;
    if (pathname.endsWith('/pdf')) {
      await route.fulfill({ contentType: 'application/pdf', body: '%PDF-1.4 document' });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '601', customerId: '101', serviceRecordId: '401', type: 'INSPECTION', fileName: 'Inspection report.pdf', contentType: 'application/pdf', storageKey: 'generated/inspection-report.pdf' }))
    });
  });

  await page.route('**/api/admin/users', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok([admin, employee])) });
  });

  await page.route('**/api/admin/loyalty-rules', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok([{ id: '701', name: 'Repeat visit reward', pointsPerDenar: 0.01, active: true }])) });
  });
}

async function signIn(page: Page, user = admin) {
  await page.addInitScript((storedUser) => {
    window.sessionStorage.setItem('carcare.accessToken', 'access-token');
    window.sessionStorage.setItem('carcare.refreshToken', 'refresh-token');
    window.sessionStorage.setItem('carcare.user', JSON.stringify(storedUser));
  }, user);
}

test.beforeEach(async ({ page }) => {
  await mockBackend(page);
});

test('redirects unauthenticated users to login', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.locator('input[name="email"]')).toBeVisible();
  await expect(page.locator('input[name="password"]')).toBeVisible();
});

test('logs in and opens the dashboard summary', async ({ page }) => {
  await page.goto('/login');

  await page.locator('input[name="email"]').fill('admin@carcare.test');
  await page.locator('input[name="password"]').fill('password123');
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL('http://127.0.0.1:5173/');
  await expect(page.getByTestId('app-toast')).toHaveCount(0);
  await expect(page.getByTestId('dashboard-hero-card')).toHaveCSS('background-image', 'none');
  await expect(page.getByTestId('dashboard-hero-card')).toHaveCSS('background-color', 'rgb(20, 35, 31)');
  await expect(page.getByText('Track customers, vehicles, appointments, service records, and offers from one workspace.')).toBeVisible();
  await expect(page.getByText('Customers', { exact: true })).toBeVisible();
  await expect(page.getByText('Vehicles', { exact: true })).toBeVisible();
});

test('shows specific login validation errors for missing email and wrong password', async ({ page }) => {
  await page.unroute('**/api/auth/login');
  await page.route('**/api/auth/login', async (route) => {
    const body = route.request().postDataJSON() as { email: string; password: string };
    if (body.email !== admin.email) {
      await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify(fail('User with that email does not exist')) });
      return;
    }
    if (body.password !== 'password123') {
      await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify(fail('Password is wrong')) });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ accessToken: 'access-token', refreshToken: 'refresh-token', user: admin }))
    });
  });

  await page.goto('/login');
  await page.locator('input[name="email"]').fill('missing@carcare.test');
  await page.locator('input[name="password"]').fill('password123');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByRole('alert')).toContainText('User with that email does not exist');

  await page.locator('input[name="email"]').fill(admin.email);
  await page.locator('input[name="password"]').fill('wrong-password');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByRole('alert')).toContainText('Password is wrong');
});

test('shows implemented authenticated module pages', async ({ page }) => {
  await signIn(page);

  const textChecks: Array<[string, string]> = [
    ['/customers', 'Ada Lovelace'],
    ['/vehicles', 'Volkswagen Golf'],
    ['/services', 'Oil and filters'],
    ['/offers', 'Brake inspection'],
    ['/documents', 'Inspection report'],
    ['/admin', 'Integration Technician']
  ];

  for (const [path, expectedText] of textChecks) {
    await page.goto(path);
    await expect(page.getByText(expectedText).first()).toBeVisible();
  }

  await page.goto('/appointments');
  await expect(page.locator('.fc')).toBeVisible();
});

test('hides and blocks admin page for non-admin users', async ({ page }) => {
  let adminUsersRequested = false;
  await page.route('**/api/admin/users**', async (route) => {
    adminUsersRequested = true;
    await route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify(fail('Forbidden')) });
  });
  await signIn(page, employee);

  await page.goto('/');
  await expect(page.locator('a[href="/admin"]')).toHaveCount(0);

  await page.goto('/admin');
  await expect(page).toHaveURL('http://127.0.0.1:5173/');
  await expect(page.locator('a[href="/admin"]')).toHaveCount(0);
  expect(adminUsersRequested).toBe(false);
});

test('creates a customer with the backend DTO shape expected by the API client', async ({ page }) => {
  await signIn(page);
  let customerPayload: Record<string, unknown> | undefined;

  await page.route('**/api/customers', async (route) => {
    if (route.request().method() === 'POST') {
      customerPayload = route.request().postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '999', ...customerPayload }))
      });
      return;
    }
    await route.fallback();
  });

  await page.goto('/customers/new');
  await page.locator('input[name="name"]').fill('Grace Hopper');
  await page.locator('input[name="phone"]').fill('+38970222222');
  await page.locator('input[name="email"]').fill('grace@carcare.test');
  await page.locator('textarea[name="notes"]').fill('Compiler Street');
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL(/\/customers\/999$/);
  expect(customerPayload).toEqual({
    fullName: 'Grace Hopper',
    email: 'grace@carcare.test',
    phone: '+38970222222',
    address: 'Compiler Street'
  });
});

test('searches customers and shows customer vehicles and service history', async ({ page }) => {
  await signIn(page);
  let searchUrl = '';
  let deletedCustomerId = '';

  await page.route('**/api/customers**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname === '/api/customers' && request.method() === 'GET') {
      searchUrl = url.toString();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '101', firstName: 'Ada', lastName: 'Lovelace', fullName: 'Ada Lovelace', email: 'ada@carcare.test', phone: '+38970111111', address: 'Analytical Lane' }]))
      });
      return;
    }
    if (url.pathname.endsWith('/vehicles')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '201', customerId: '101', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123' }]))
      });
      return;
    }
    if (url.pathname.endsWith('/service-history')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '201', serviceDate: '2026-06-12', serviceType: 'Oil and filters', totalAmount: 3500, odometer: 123456 }]))
      });
      return;
    }
    if (request.method() === 'DELETE') {
      deletedCustomerId = url.pathname.split('/').pop() ?? '';
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null, 'Deleted')) });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '101', firstName: 'Ada', lastName: 'Lovelace', fullName: 'Ada Lovelace', email: 'ada@carcare.test', phone: '+38970111111', address: 'Analytical Lane' }))
    });
  });

  await page.goto('/customers');
  await page.locator('input[name="firstName"]').fill('Ada');
  await page.locator('input[name="lastName"]').fill('Lovelace');
  await page.getByRole('button', { name: 'Search' }).click();

  await expect(page.getByText('Ada Lovelace').first()).toBeVisible();
  expect(searchUrl).toContain('firstName=Ada');
  expect(searchUrl).toContain('lastName=Lovelace');

  await page.getByRole('link', { name: 'Details' }).click();
  await expect(page.getByText('Customer vehicles')).toBeVisible();
  await expect(page.getByText('SK-1234-AA')).toBeVisible();
  await expect(page.getByText('Service history')).toBeVisible();
  await expect(page.getByText('Oil and filters')).toBeVisible();

  await page.getByRole('button', { name: 'Delete' }).click();
  await expect(page).toHaveURL(/\/customers$/);
  expect(deletedCustomerId).toBe('101');
});

test('searches creates and updates vehicles with backend DTO mapping', async ({ page }) => {
  await signIn(page);
  const backendVehicles = [
    { id: '201', customerId: '101', plateNumber: 'SK-1234-AA', make: 'Volkswagen', model: 'Golf', modelYear: 2020, vin: 'VIN123' }
  ];
  let searchUrl = '';
  let createPayload: Record<string, unknown> | undefined;
  let updatePayload: Record<string, unknown> | undefined;

  await page.route('**/api/vehicles**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname === '/api/vehicles' && request.method() === 'GET') {
      searchUrl = url.toString();
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(backendVehicles)) });
      return;
    }
    if (url.pathname === '/api/vehicles' && request.method() === 'POST') {
      createPayload = request.postDataJSON();
      const created = { id: '202', ...createPayload };
      backendVehicles.push(created as (typeof backendVehicles)[number]);
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(created)) });
      return;
    }
    if (request.method() === 'PUT') {
      updatePayload = request.postDataJSON();
      const id = url.pathname.split('/').pop() ?? '';
      const index = backendVehicles.findIndex((vehicle) => vehicle.id === id);
      backendVehicles[index] = { id, ...updatePayload } as (typeof backendVehicles)[number];
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(backendVehicles[index])) });
      return;
    }
    if (url.pathname.endsWith('/service-history')) {
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '202', serviceDate: '2026-06-12', serviceType: 'Major Service', partsCost: 2500, laborCost: 1500, totalAmount: 4000, odometer: 123456, replacedParts: 'Air filter' }]))
      });
      return;
    }
    const id = url.pathname.split('/').pop() ?? '';
    const vehicle = backendVehicles.find((item) => item.id === id) ?? backendVehicles[0];
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(vehicle)) });
  });

  await page.goto('/vehicles');
  await page.locator('input[name="vin"]').fill('VIN123');
  await page.locator('input[name="plateNumber"]').fill('SK-1234-AA');
  await page.locator('input[name="owner"]').fill('Ada');
  await page.getByRole('button', { name: 'Search' }).click();

  await expect(page.getByText('Volkswagen Golf').first()).toBeVisible();
  expect(searchUrl).toContain('vin=VIN123');
  expect(searchUrl).toContain('plateNumber=SK-1234-AA');
  expect(searchUrl).toContain('owner=Ada');

  await page.goto('/vehicles/new');
  await page.locator('input[name="customerId"]').fill('101');
  await page.locator('input[name="plate"]').fill('OH-2020-AA');
  await page.locator('input[name="make"]').fill('Toyota');
  await page.locator('input[name="model"]').fill('Corolla');
  await page.locator('input[name="year"]').fill('2022');
  await page.locator('input[name="vin"]').fill('VIN202');
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL(/\/vehicles\/202$/);
  expect(createPayload).toEqual({
    customerId: '101',
    plateNumber: 'OH-2020-AA',
    make: 'Toyota',
    model: 'Corolla',
    modelYear: 2022,
    vin: 'VIN202'
  });

  await page.goto('/vehicles/202/edit');
  await page.locator('input[name="plate"]').fill('OH-7777-AA');
  await page.locator('input[name="year"]').fill('2023');
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL(/\/vehicles\/202$/);
  expect(updatePayload).toMatchObject({
    customerId: '101',
    plateNumber: 'OH-7777-AA',
    make: 'Toyota',
    model: 'Corolla',
    modelYear: 2023,
    vin: 'VIN202'
  });
});

test('records services with parts labor and replaced parts', async ({ page }) => {
  await signIn(page);
  let servicePayload: Record<string, unknown> | undefined;

  await page.route('**/api/service-records', async (route) => {
    const request = route.request();
    if (request.method() === 'POST') {
      servicePayload = request.postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '402', ...servicePayload, totalAmount: 3500 }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '401', customerId: '101', vehicleId: '201', serviceDate: '2026-06-12', serviceType: 'Oil and filters', partsCost: 1500, laborCost: 2000, totalAmount: 3500, odometer: 123456, replacedParts: 'Oil filter' }]))
    });
  });

  await page.goto('/services/new');
  await page.locator('input[name="customerId"]').fill('101');
  await page.locator('input[name="vehicleId"]').fill('201');
  await page.locator('input[name="performedAt"]').fill('12.06.2026.');
  await page.locator('input[name="serviceTime"]').fill('14:35:22');
  await page.locator('input[name="mileage"]').fill('123456');
  await page.locator('input[name="summary"]').fill('Part Replacement Service');
  await page.locator('input[name="replacedParts"]').fill('Oil filter');
  await page.locator('input[name="partsCost"]').fill('1500');
  await page.locator('input[name="laborCost"]').fill('2000');
  await page.locator('textarea[name="notes"]').fill('Oil and filters');
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL(/\/services$/);
  expect(servicePayload).toEqual({
    customerId: '101',
    vehicleId: '201',
    serviceDate: '2026-06-12',
    serviceType: 'Part Replacement Service',
    partsCost: 1500,
    laborCost: 2000,
    odometer: 123456,
    replacedParts: 'Oil filter',
    notes: 'Oil and filters'
  });
  expect(servicePayload).not.toHaveProperty('serviceTime');
});

test('shows available appointments and schedules without conflicts', async ({ page }) => {
  await signIn(page);
  let appointmentPayload: Record<string, unknown> | undefined;
  let availableUrl = '';

  await page.route('**/api/appointments/available**', async (route) => {
    availableUrl = route.request().url();
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ startsAt: '2026-06-20T09:00:00.000+02:00', endsAt: '2026-06-20T10:00:00.000+02:00' }]))
    });
  });

  await page.route('**/api/appointments', async (route) => {
    const request = route.request();
    if (request.method() === 'POST') {
      appointmentPayload = request.postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '302', customerId: '101', vehicleId: '201', scheduledAt: appointmentPayload?.startsAt, endsAt: appointmentPayload?.endsAt, serviceType: appointmentPayload?.title, status: 'SCHEDULED', cancellationUrl: '/api/appointments/cancel/token-302' }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '301', customerId: '101', vehicleId: '201', scheduledAt: '2026-06-15T09:00:00.000Z', endsAt: '2026-06-15T10:00:00.000Z', serviceType: 'Oil change', status: 'SCHEDULED' }]))
    });
  });

  await page.goto('/appointments');
  await expect(page.getByLabel('Select available date')).toBeVisible();
  await expect(page.getByLabel('Select start date')).toBeVisible();
  await expect(page.getByLabel('Select start time')).toBeVisible();
  await expect(page.getByLabel('Select end date')).toBeVisible();
  await expect(page.getByLabel('Select end time')).toBeVisible();
  await page.locator('input[name="slotDate"]').fill('20.06.2026.');
  const availableSlot = page.locator('.MuiChip-root').filter({ hasText: '09:00:00' });
  await expect(availableSlot).toBeVisible();
  expect(availableUrl).toContain('date=2026-06-20');

  await availableSlot.click();
  await page.locator('input[name="customerId"]').fill('101');
  await page.locator('input[name="vehicleId"]').fill('201');
  await page.locator('input[name="title"]').fill('Minor Service');
  await expect(page.locator('input[name="startsAt"]')).toHaveValue('20.06.2026. 09:00:00');
  await expect(page.locator('input[name="endsAt"]')).toHaveValue('20.06.2026. 10:00:00');
  await page.locator('button[type="submit"]').click();

  expect(appointmentPayload).toMatchObject({
    customerId: '101',
    vehicleId: '201',
    title: 'Minor Service'
  });
  expect(appointmentPayload?.startsAt).toBe('2026-06-20T09:00:00+02:00');
  expect(appointmentPayload?.endsAt).toBe('2026-06-20T10:00:00+02:00');
});

test('creates and sends quotations with a detailed cost breakdown', async ({ page }) => {
  await signIn(page);
  let offerPayload: Record<string, unknown> | undefined;
  let offerStatus = 'DRAFT';
  let sendCalled = false;
  let quotePdfCalled = false;

  await page.route('**/api/offers', async (route) => {
    const request = route.request();
    if (request.method() === 'POST') {
      offerPayload = request.postDataJSON();
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '502', ...offerPayload, amount: 1200, status: 'DRAFT' }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '501', customerId: '101', vehicleId: '201', title: 'Brake inspection', partsCost: 700, laborCost: 500, amount: 1200, status: 'DRAFT' }]))
    });
  });

  await page.route('**/api/offers/**', async (route) => {
    const pathname = new URL(route.request().url()).pathname;
    if (pathname.endsWith('/pdf')) {
      quotePdfCalled = true;
      await route.fulfill({ contentType: 'application/pdf', body: '%PDF-1.4 quote' });
      return;
    }
    if (pathname.endsWith('/send')) {
      offerStatus = 'SENT';
      sendCalled = true;
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: '502', customerId: '101', vehicleId: '201', title: 'Brake inspection', partsCost: 700, laborCost: 500, amount: 1200, status: offerStatus }))
      });
      return;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '502', customerId: '101', vehicleId: '201', title: 'Brake inspection', partsCost: 700, laborCost: 500, amount: 1200, status: offerStatus }))
    });
  });

  await page.goto('/offers/new');
  await page.locator('input[name="customerId"]').fill('101');
  await page.locator('input[name="vehicleId"]').fill('201');
  await page.locator('input[name="title"]').fill('Brake inspection');
  await page.locator('input[name="partsCost"]').fill('700');
  await page.locator('input[name="laborCost"]').fill('500');
  await page.locator('button[type="submit"]').click();

  await expect(page).toHaveURL(/\/offers\/502$/);
  expect(offerPayload).toEqual({
    customerId: '101',
    vehicleId: '201',
    title: 'Brake inspection',
    partsCost: 700,
    laborCost: 500
  });

  await page.goto('/offers/502');
  await expect(page.getByText('Parts cost')).toBeVisible();
  await page.getByRole('button', { name: 'PDF' }).click();
  await expect.poll(() => quotePdfCalled).toBeTruthy();
  await page.getByRole('button', { name: 'Испрати' }).click();
  await expect.poll(() => sendCalled).toBeTruthy();
});

test('sends generated service documents and exports PDFs', async ({ page }) => {
  await signIn(page);
  let documentPdfCalled = false;
  let documentSendCalled = false;

  await page.route('**/api/documents', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([{ id: '601', customerId: '101', serviceRecordId: '401', type: 'INSPECTION', fileName: 'Inspection report.pdf', contentType: 'application/pdf', storageKey: 'generated/inspection-report.pdf' }]))
    });
  });

  await page.route('**/api/documents/**', async (route) => {
    const pathname = new URL(route.request().url()).pathname;
    if (pathname.endsWith('/pdf')) {
      documentPdfCalled = true;
      await route.fulfill({ contentType: 'application/pdf', body: '%PDF-1.4 document' });
      return;
    }
    if (pathname.endsWith('/send')) {
      documentSendCalled = true;
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ id: '601', customerId: '101', serviceRecordId: '401', type: 'INSPECTION', fileName: 'Inspection report.pdf', contentType: 'application/pdf', storageKey: 'generated/inspection-report.pdf' }))
    });
  });

  await page.goto('/documents');
  await expect(page.getByText('Inspection report.pdf')).toBeVisible();
  await page.locator('tbody button').nth(1).click();
  await expect.poll(() => documentPdfCalled).toBeTruthy();
  await page.locator('tbody button').nth(2).click();
  await expect.poll(() => documentSendCalled).toBeTruthy();
});

test('creates updates and disables employee accounts from admin', async ({ page }) => {
  await signIn(page);
  const users = [admin, employee];
  let createPayload: Record<string, unknown> | undefined;
  let updatePayload: Record<string, unknown> | undefined;
  let deletedUserId: string | undefined;

  await page.route('**/api/admin/users**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (request.method() === 'GET') {
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(users)) });
      return;
    }
    if (request.method() === 'POST') {
      createPayload = request.postDataJSON();
      const created = { id: '3', failedLoginAttempts: 0, ...createPayload };
      users.push(created as typeof admin);
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(created)) });
      return;
    }
    if (request.method() === 'PUT') {
      updatePayload = request.postDataJSON();
      const id = url.pathname.split('/').pop() ?? '';
      const index = users.findIndex((user) => user.id === id);
      users[index] = { ...users[index], ...updatePayload };
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(users[index])) });
      return;
    }
    if (request.method() === 'DELETE') {
      deletedUserId = url.pathname.split('/').pop();
      const index = users.findIndex((user) => user.id === deletedUserId);
      users[index] = { ...users[index], enabled: false };
      await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null)) });
      return;
    }
    await route.fallback();
  });

  await page.goto('/admin');
  await expect(page.locator('input[name="fullName"]')).toHaveCount(0);
  await page.getByRole('button', { name: 'Create employee' }).click();
  await page.locator('input[name="fullName"]').fill('New Technician');
  await page.locator('input[name="email"]').fill('new-tech@carcare.test');
  await page.locator('input[name="password"]').fill('password123');
  await page.locator('select[name="enabled"]').selectOption('true');
  await page.locator('select[name="role"]').selectOption('EMPLOYEE');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByText('new-tech@carcare.test')).toBeVisible();
  expect(createPayload).toMatchObject({
    fullName: 'New Technician',
    email: 'new-tech@carcare.test',
    password: 'password123',
    enabled: true,
    roles: ['ROLE_EMPLOYEE']
  });

  await page.getByLabel('edit new-tech@carcare.test').click();
  await page.locator('input[name="fullName"]').fill('Updated Technician');
  await page.locator('select[name="enabled"]').selectOption('false');
  await page.locator('select[name="role"]').selectOption('ADMIN');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByText('Updated Technician')).toBeVisible();
  expect(updatePayload).toMatchObject({
    fullName: 'Updated Technician',
    email: 'new-tech@carcare.test',
    enabled: false,
    roles: ['ROLE_ADMIN']
  });

  await page.getByLabel('delete new-tech@carcare.test').click();
  await expect(page.getByRole('dialog', { name: 'Delete employee' })).toBeVisible();
  await page.getByRole('button', { name: 'Delete' }).click();
  await expect(page.getByText('DISABLED').first()).toBeVisible();
  expect(deletedUserId).toBe('3');
});

test('changes password and logs out through the authenticated shell', async ({ page }) => {
  await signIn(page);
  await page.goto('/change-password');

  await page.locator('input[name="currentPassword"]').fill('password123');
  await page.locator('input[name="newPassword"]').fill('password456');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByText(/success|успешно|Ð£ÑÐ¿ÐµÑˆÐ½Ð¾/i).first()).toBeVisible();
  const successToast = page.getByTestId('app-toast');
  await expect(successToast).toHaveCount(1);
  await expect(successToast).toHaveCSS('background-color', 'rgb(21, 128, 61)');
  await expect(successToast).toHaveCSS('background-image', 'none');
  await expect(page.locator('.MuiAlert-root')).toHaveCount(0);
  await page.locator('button:has([data-testid="LogoutRoundedIcon"])').click();
  await expect(page).toHaveURL(/\/login$/);
});

test('shows an inline error when current password is wrong', async ({ page }) => {
  await signIn(page);
  await page.unroute('**/api/auth/change-password');
  await page.route('**/api/auth/change-password', async (route) => {
    await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify(fail('Invalid current password')) });
  });

  await page.goto('/change-password');
  await page.locator('input[name="currentPassword"]').fill('wrong-password');
  await page.locator('input[name="newPassword"]').fill('password456');
  await page.locator('button[type="submit"]').click();

  await expect(page.getByRole('alert')).toContainText('Invalid current password');
  await expect(page.getByTestId('app-toast')).toHaveCount(0);
});
