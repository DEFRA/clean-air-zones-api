const AWS = require('aws-sdk')

const USERS_PAGE_LIMIT = 10
const DELAY_INTERVAL_IN_MS = 1000

const client = new AWS.CognitoIdentityServiceProvider({apiVersion: '2016-04-18'})

///------ Main method

const userPoolId = fetchUserPoolId()
processPool()

///------

function fetchUserPoolId() {
  const args = process.argv
  if (args.length != 3) {
    throw 'You should provide one argument which is the user-pool-id!'
  }
  return args[2]
}

async function processPool() {
  console.log(`About to start processing users of pool ${userPoolId}`)
  for await (const usersPage of usersOfPool()) {
    await processUsers(usersPage['Users'])
  }
}

async function processUsers(users) {
  for (const user of users) {
    await processUser(user)
    await sleep(DELAY_INTERVAL_IN_MS)
  }
}

async function* usersOfPool() {
  const usersResponse = await client.listUsers({UserPoolId: userPoolId, Limit: USERS_PAGE_LIMIT}).promise()
  yield usersResponse
  yield* paginatedUsersOfPool(usersResponse['PaginationToken'])
}

async function* paginatedUsersOfPool(paginationToken) {
  if (paginationToken) {
    const usersResponse = await client.listUsers({UserPoolId: userPoolId, Limit: USERS_PAGE_LIMIT, PaginationToken: paginationToken}).promise()
    yield usersResponse
    yield* paginatedUsersOfPool(usersResponse['PaginationToken'])
  }
}

async function processUser(user) {
  const sub = getUserAttributeValue(user, 'sub')
  try {
    await client.adminUpdateUserAttributes({
      UserAttributes: [
        {
          Name: 'preferred_username',
          Value: sub
        }
      ],
      UserPoolId: userPoolId,
      Username: user['Username']
    }).promise()
    console.log(`Successfully processed user with sub ${sub}`)
  } catch(e) {
    console.error(`Updating user attributes for user ${sub} failed: ${e.message}`, e)
  }
}

function getUserAttributeValue(user, attributeName) {
  const attribute = user['Attributes'].find(attr => attr['Name'] === attributeName)
  return attribute['Value']
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
