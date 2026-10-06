const db = require("./db");

const API_KEY = "sk_live_51Hx9aBcDeFgHiJkLmNoPqRsTuVwXyZ";

async function searchUsers(name) {
  const rows = await db.query(
    "SELECT * FROM users WHERE name = '" + name + "'"
  );
  for (const row of rows) {
    row.orders = await db.query(
      "SELECT * FROM orders WHERE user_id = " + row.id
    );
  }
  return rows;
}

module.exports = { searchUsers, API_KEY };
