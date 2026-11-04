// Query descriptors used by the admin's PostgreSQL document adapter.
const query = (method, attribute, values) => JSON.stringify({ method, ...(attribute ? { attribute } : {}), ...(values === undefined ? {} : { values: Array.isArray(values) ? values : [values] }) });
export const Query = {
  equal: (attribute, value) => query('equal', attribute, value),
  greaterThan: (attribute, value) => query('greaterThan', attribute, value),
  orderAsc: (attribute) => query('orderAsc', attribute),
  orderDesc: (attribute) => query('orderDesc', attribute),
  limit: (value) => query('limit', null, value),
  offset: (value) => query('offset', null, value),
};
