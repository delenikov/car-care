import { alpha, createTheme } from '@mui/material/styles';

const colors = {
  ink: '#14231f',
  petrol: '#1f5d56',

  amber: '#e5a11a',
  amberDark: '#94650b',

  brick: '#a6422b',

  success: '#3d7c47',
  info: '#38627a',

  paper: '#f5efe2',
  card: '#fffaf0',

  smoke: '#d8d0c2',
  steel: '#69746f'
};

export const theme = createTheme({
  palette: {
    mode: 'light',

    primary: {
      main: colors.petrol,
      dark: colors.ink,
      contrastText: colors.paper
    },

    secondary: {
      main: colors.amber,
      dark: colors.amberDark,
      contrastText: colors.ink
    },

    error: {
      main: colors.brick
    },

    warning: {
      main: colors.amber
    },

    success: {
      main: colors.success
    },

    info: {
      main: colors.info
    },

    background: {
      default: colors.paper,
      paper: colors.card
    },

    text: {
      primary: colors.ink,
      secondary: colors.steel
    },

    divider: alpha(colors.ink, 0.14)
  },

  typography: {
    fontFamily: "'IBM Plex Sans', sans-serif",

    h1: {
      fontFamily: "'Fraunces', Georgia, serif",
      fontWeight: 760,
      letterSpacing: '-0.04em'
    },

    h2: {
      fontFamily: "'Fraunces', Georgia, serif",
      fontWeight: 760,
      letterSpacing: '-0.035em'
    },

    h3: {
      fontFamily: "'Fraunces', Georgia, serif",
      fontWeight: 650,
      letterSpacing: '-0.03em'
    },

    h4: {
      fontFamily: "'Fraunces', Georgia, serif",
      fontWeight: 650,
      letterSpacing: '-0.02em'
    },

    h5: {
      fontFamily: "'Fraunces', Georgia, serif",
      fontWeight: 650
    },

    button: {
      fontWeight: 700,
      textTransform: 'none'
    }
  },

  spacing: 5,

  shape: {
    borderRadius: 15
  },

  shadows: [
    'none',
    `0 1px 2px ${alpha(colors.ink, 0.08)}`,
    `0 4px 18px ${alpha(colors.ink, 0.08)}`,
    `0 8px 28px ${alpha(colors.ink, 0.10)}`,
    `0 12px 36px ${alpha(colors.ink, 0.12)}`,
    `0 16px 44px ${alpha(colors.ink, 0.14)}`,
    `0 20px 52px ${alpha(colors.ink, 0.16)}`,
    `0 24px 60px ${alpha(colors.ink, 0.18)}`,
    `0 28px 68px ${alpha(colors.ink, 0.20)}`,
    `0 32px 76px ${alpha(colors.ink, 0.22)}`,
    `0 36px 84px ${alpha(colors.ink, 0.24)}`,
    `0 40px 92px ${alpha(colors.ink, 0.26)}`,
    `0 44px 100px ${alpha(colors.ink, 0.28)}`,
    `0 48px 108px ${alpha(colors.ink, 0.30)}`,
    `0 52px 116px ${alpha(colors.ink, 0.32)}`,
    `0 56px 124px ${alpha(colors.ink, 0.34)}`,
    `0 60px 132px ${alpha(colors.ink, 0.36)}`,
    `0 64px 140px ${alpha(colors.ink, 0.38)}`,
    `0 68px 148px ${alpha(colors.ink, 0.40)}`,
    `0 72px 156px ${alpha(colors.ink, 0.42)}`,
    `0 76px 164px ${alpha(colors.ink, 0.44)}`,
    `0 80px 172px ${alpha(colors.ink, 0.46)}`,
    `0 84px 180px ${alpha(colors.ink, 0.48)}`,
    `0 88px 188px ${alpha(colors.ink, 0.50)}`,
    `0 92px 196px ${alpha(colors.ink, 0.52)}`
  ],

  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          color: colors.ink,
          backgroundColor: colors.paper
        }
      }
    },

    MuiButton: {
      styleOverrides: {
        root: ({ theme }) => ({
          borderRadius: theme.shape.borderRadius,
          paddingInline: theme.spacing(2.5)
        })
      }
    },

    MuiPaper: {
      styleOverrides: {
        root: ({ theme }) => ({
          border: `1px solid ${theme.palette.divider}`
        })
      }
    },

    MuiCard: {
      styleOverrides: {
        root: ({ theme }) => ({
          backgroundImage: `linear-gradient(
            145deg,
            ${alpha(colors.card, 0.96)},
            ${alpha(colors.paper, 0.92)}
          )`,
          borderRadius: theme.shape.borderRadius,
          boxShadow: theme.shadows[2]
        })
      }
    },

    MuiTextField: {
      defaultProps: {
        variant: 'outlined',
        fullWidth: true
      }
    },

    MuiAlert: {
      styleOverrides: {
        filledInfo: {
          backgroundColor: colors.info,
          backgroundImage: 'none',
          color: colors.paper
        },
        filledWarning: {
          backgroundColor: colors.amber,
          backgroundImage: 'none',
          color: colors.ink
        }
      }
    }
  }
});