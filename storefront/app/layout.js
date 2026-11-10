import './styles.css';

export const metadata = {
  title: 'Ecommerce Store',
  description: 'Browse the catalog, save favorites, and check out securely.'
};

export default function RootLayout({ children }) {
  return <html lang="en"><body>{children}</body></html>;
}
