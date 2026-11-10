// Generate SQL for the local MySQL demo catalog. Existing records are preserved.
const categories = [
  'Mobiles|Smartphone|Foldable Phone|5G Phone|Compact Phone',
  'Laptops|Notebook|Gaming Laptop|Ultrabook|Convertible Laptop',
  'Tablets|Android Tablet|Drawing Tablet|Mini Tablet|Reading Tablet',
  'Headphones|Wireless Headphones|Studio Headphones|Earbuds|Sport Earbuds',
  'Speakers|Bluetooth Speaker|Soundbar|Desktop Speaker|Party Speaker',
  'Cameras|Mirrorless Camera|Action Camera|Instant Camera|Webcam',
  'Televisions|Smart TV|OLED TV|LED TV|Streaming Display',
  'Monitors|Gaming Monitor|Office Monitor|Portable Monitor|Curved Monitor',
  'Computer Accessories|Keyboard|Wireless Mouse|USB Hub|Laptop Stand',
  'Gaming|Gamepad|Gaming Headset|Gaming Chair|Arcade Controller',
  'Smart Watches|Fitness Watch|Outdoor Watch|Smart Band|Hybrid Watch',
  'Appliances|Air Purifier|Vacuum Cleaner|Steam Iron|Water Purifier',
  'Kitchen|Mixer Grinder|Electric Kettle|Air Fryer|Toaster',
  'Cookware|Frying Pan|Stock Pot|Saucepan|Tawa',
  'Dining|Dinner Set|Serving Bowl|Cutlery Set|Glass Set',
  'Home|Ceramic Cup|Table Clock|Storage Tray|Cushion Cover',
  'Furniture|Desk|Side Table|Bookshelf|Accent Chair',
  'Lighting|Desk Lamp|Wall Lamp|Floor Lamp|LED Strip',
  'Bedding|Cotton Sheet|Pillow Set|Quilt|Mattress Protector',
  'Bath|Bath Towel|Shower Caddy|Bath Mat|Soap Dispenser',
  'Decor|Wall Art|Vase|Photo Frame|Candle Holder',
  'Garden|Planter|Watering Can|Garden Shears|Hose Reel',
  'Tools|Tool Kit|Screwdriver Set|Cordless Drill|Measuring Tape',
  'Men’s Clothing|Oxford Shirt|Polo Shirt|Denim Jeans|Chino Trousers',
  'Women’s Clothing|Linen Top|Maxi Dress|Straight Jeans|Knit Cardigan',
  'Kids’ Clothing|Cotton Tee|Joggers|Hoodie|Summer Dress',
  'Footwear|Running Shoes|Walking Sandals|Loafers|Canvas Sneakers',
  'Accessories|Canvas Bag|Leather Wallet|Belt|Sunglasses',
  'Bags & Luggage|Travel Backpack|Carry-on Case|Duffel Bag|Laptop Bag',
  'Jewellery|Silver Pendant|Stud Earrings|Chain Bracelet|Stack Ring',
  'Watches|Analog Watch|Chronograph|Dress Watch|Digital Watch',
  'Beauty|Face Cleanser|Moisturiser|Sunscreen|Lip Balm',
  'Personal Care|Hair Dryer|Trimmer|Electric Toothbrush|Hair Brush',
  'Fragrance|Eau de Parfum|Body Mist|Cologne|Perfume Set',
  'Sports|Cricket Bat|Football|Badminton Racket|Basketball',
  'Fitness|Yoga Mat|Resistance Bands|Dumbbells|Skipping Rope',
  'Outdoor|Camping Lantern|Tent|Trekking Poles|Sleeping Bag',
  'Cycling|Cycle Helmet|Bike Pump|Bike Light|Saddle Bag',
  'Books|Travel Guide|Cookbook|Field Journal|Design Handbook',
  'Stationery|Gel Pen|Sketchbook|Desk Planner|Pencil Case',
  'Arts & Crafts|Watercolor Set|Craft Kit|Brush Set|Drawing Pad',
  'Toys|Building Blocks|Puzzle|Toy Car|Board Game',
  'Baby|Feeding Bottle|Baby Blanket|Play Mat|Travel Carrier',
  'Pets|Pet Bowl|Pet Bed|Leash|Grooming Brush',
  'Automotive|Car Charger|Car Cover|Dash Camera|Tyre Inflator',
  'Travel|Passport Holder|Packing Cubes|Neck Pillow|Travel Adapter',
  'Groceries|Coffee Beans|Breakfast Oats|Tea Pack|Spice Set',
  'Essentials|Steel Tumbler|Lunch Box|Travel Flask|Storage Jar',
  'Musical Instruments|Acoustic Guitar|Keyboard Instrument|Ukulele|Practice Pad',
  'Office|Office Chair|Standing Desk|Document Scanner|Conference Display'
];
const variants = ['Core', 'Select', 'Plus', 'Pro', 'Signature'];
const ranges = [
  [6999, 95000], [24999, 100000], [4999, 65000], [399, 25000], [499, 35000],
  [899, 90000], [8999, 95000], [5499, 60000], [199, 8500], [699, 22000],
  [899, 26000], [599, 28000], [699, 16000], [249, 9500], [199, 7000],
  [149, 5500], [1499, 48000], [299, 12000], [349, 8500], [199, 5500],
  [249, 7500], [149, 5500], [249, 10500], [399, 4200], [399, 6200],
  [249, 3200], [399, 8500], [199, 4800], [499, 16500], [399, 20000],
  [599, 22000], [149, 3200], [129, 9500], [249, 9500], [399, 8500],
  [199, 11500], [499, 16500], [199, 7500], [149, 1800], [50, 1250],
  [99, 3600], [149, 6500], [199, 8500], [149, 4800], [249, 14500],
  [199, 6500], [99, 1700], [149, 2400], [699, 36000], [1499, 85000]
];
const quote = value => "'" + value.replaceAll("'", "''") + "'";
const rows = categories.flatMap((entry, category) => {
  const [name, ...types] = entry.split('|');
  return Array.from({ length: 20 }, (_, index) => {
    const title = types[Math.floor(index / 5)] + ' ' + variants[index % 5];
    const [minimum, maximum] = ranges[category];
    const price = Math.round(minimum + (maximum - minimum) * (index / 19) ** 2);
    return '(' + [quote(name), index + 1, quote(title),
      quote(title + ' from the ' + name + ' collection. Demo catalog product.'), price].join(',') + ')';
  });
});
process.stdout.write([
  'SET NAMES utf8mb4;',
  'CREATE TEMPORARY TABLE demo_catalog (category VARCHAR(100), slot INT, title VARCHAR(255), description VARCHAR(255), price DECIMAL(12,2));',
  'INSERT INTO demo_catalog VALUES\n' + rows.join(',\n') + ';',
  'START TRANSACTION;',
  'INSERT INTO category(name) SELECT DISTINCT d.category FROM demo_catalog d WHERE NOT EXISTS (SELECT 1 FROM category c WHERE c.name = d.category);',
  'CREATE TEMPORARY TABLE demo_counts AS SELECT c.id, COUNT(p.id) AS current_count FROM category c LEFT JOIN product p ON p.category_id=c.id GROUP BY c.id;',
  'INSERT INTO product(category_id,title,description,image,price) SELECT c.id,d.title,d.description,\'\',d.price FROM demo_catalog d JOIN category c ON c.name=d.category JOIN demo_counts n ON n.id=c.id WHERE d.slot <= 20-n.current_count AND NOT EXISTS (SELECT 1 FROM product p WHERE p.category_id=c.id AND p.title=d.title) ORDER BY c.id,d.slot;',
  'INSERT INTO inventory_item(product_id,available,reserved) SELECT p.id,12,0 FROM product p JOIN category c ON c.id=p.category_id WHERE c.name IN (SELECT d.category FROM demo_catalog d) AND NOT EXISTS (SELECT 1 FROM inventory_item i WHERE i.product_id=p.id);',
  'COMMIT;',
  'SELECT COUNT(*) AS products, COUNT(DISTINCT category_id) AS categories, MIN(price) AS minimum_price, MAX(price) AS maximum_price FROM product;'
].join('\n') + '\n');
