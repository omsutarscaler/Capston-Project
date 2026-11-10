const ecommerce = process.env.ECOMMERCE_URL || 'http://127.0.0.1:8080';
const auth = process.env.USER_AUTH_URL || 'http://127.0.0.1:8081';

export default {
  async rewrites() {
    return [
      { source: '/api/shop/:path*', destination: ecommerce + '/:path*' },
      { source: '/api/account/:path*', destination: auth + '/user/:path*' }
    ];
  },
  async headers() {
    return [{ source: '/:path*', headers: [{ key: 'Referrer-Policy', value: 'no-referrer' }] }];
  }
};
