import { alpha, createTheme } from '@mui/material/styles';

const palette = {
  ink: '#14231f',
  petrol: '#1f5d56',
  amber: '#e5a11a',
  brick: '#a6422b',
  paper: '#f5efe2',
  card: '#fffaf0',
  smoke: '#d8d0c2',
  steel: '#69746f'
};

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: palette.petrol, dark: palette.ink, contrastText: palette.paper },
    secondary: { main: palette.amber, dark: '#94650b', contrastText: palette.ink },
    error: { main: palette.brick },
    warning: { main: palette.amber },
    success: { main: '#3d7c47' },
    info: { main: '#38627a' },
    background: { default: palette.paper, paper: palette.card },
    text: { primary: palette.ink, secondary: palette.steel },
    divider: alpha(palette.ink, 0.14)
  },
  typography: {
    fontFamily: "'IBM Plex Sans', sans-serif",
    h1: { fontFamily: "'Fraunces', Georgia, serif", fontWeight: 760, letterSpacing: '-0.04em' },
    h2: { fontFamily: "'Fraunces', Georgia, serif", fontWeight: 760, letterSpacing: '-0.035em' },
    h3: { fontFamily: "'Fraunces', Georgia, serif", fontWeight: 650, letterSpacing: '-0.03em' },
    h4: { fontFamily: "'Fraunces', Georgia, serif", fontWeight: 650, letterSpacing: '-0.02em' },
    h5: { fontFamily: "'Fraunces', Georgia, serif", fontWeight: 650 },
    button: { fontWeight: 700, textTransform: 'none' }
  },
  shape: { borderRadius: 18 },
  spacing: 8,
  shadows: [
    'none',
    `0 1px 2px ${alpha(palette.ink, 0.08)}`,
    `0 4px 18px ${alpha(palette.ink, 0.08)}`,
    `0 8px 28px ${alpha(palette.ink, 0.1)}`,
    `0 14px 38px ${alpha(palette.ink, 0.12)}`,
    `0 18px 48px ${alpha(palette.ink, 0.14)}`,
    `0 24px 64px ${alpha(palette.ink, 0.16)}`,
    `0 28px 80px ${alpha(palette.ink, 0.18)}`,
    `0 34px 90px ${alpha(palette.ink, 0.2)}`,
    `0 38px 100px ${alpha(palette.ink, 0.22)}`,
    `0 44px 110px ${alpha(palette.ink, 0.24)}`,
    `0 50px 120px ${alpha(palette.ink, 0.26)}`,
    `0 56px 130px ${alpha(palette.ink, 0.28)}`,
    `0 62px 140px ${alpha(palette.ink, 0.3)}`,
    `0 68px 150px ${alpha(palette.ink, 0.32)}`,
    `0 74px 160px ${alpha(palette.ink, 0.34)}`,
    `0 80px 170px ${alpha(palette.ink, 0.36)}`,
    `0 86px 180px ${alpha(palette.ink, 0.38)}`,
    `0 92px 190px ${alpha(palette.ink, 0.4)}`,
    `0 98px 200px ${alpha(palette.ink, 0.42)}`,
    `0 104px 210px ${alpha(palette.ink, 0.44)}`,
    `0 110px 220px ${alpha(palette.ink, 0.46)}`,
    `0 116px 230px ${alpha(palette.ink, 0.48)}`,
    `0 122px 240px ${alpha(palette.ink, 0.5)}`,
    `0 128px 250px ${alpha(palette.ink, 0.52)}`
  ],
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          color: palette.ink
        }
      }
    },
    MuiButton: {
      styleOverrides: {
        root: ({ theme }) => ({
          borderRadius: theme.spacing(3),
          paddingInline: theme.spacing(2.5)
        })
      }
    },
    MuiPaper: {
      styleOverrides: {
        root: ({ theme }) => ({
          backgroundImage: `linear-gradient(145deg, ${alpha(palette.card, 0.96)}, ${alpha(palette.paper, 0.92)})`,
          border: `1px solid ${theme.palette.divider}`
        })
      }
    },
    MuiTextField: {
      defaultProps: { variant: 'outlined', fullWidth: true }
    },
    MuiCard: {
      styleOverrides: {
        root: ({ theme }) => ({
          borderRadius: theme.spacing(3),
          boxShadow: theme.shadows[2]
        })
      }
    }
  }
});
