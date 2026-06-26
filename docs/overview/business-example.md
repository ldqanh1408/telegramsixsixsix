# Business Example — How MyCompany Uses This App

A concrete, end-to-end walkthrough of the app's business value — no code, just operations.

---

## The cast

- **Notification server**: runs in **one single place** at `https://noti.mycompany.com`
- **Tuan** — DevOps engineer, the system admin (holds the admin token)
- **3 repos** to watch:
  - `mycompany/backend`
  - `mycompany/mobile`
  - `mycompany/infra`
- **3 Telegram groups**: "Backend Team", "Mobile Team", "DevOps"

---

## Part 1 — Preparing the bots (Tuan does this once)

### 1a. Create the bots on Telegram
Tuan chats with **@BotFather** on Telegram, creates 3 bots, and gets back 3 tokens:
- `@be_noti_bot` → token `111:AAA`
- `@mob_noti_bot` → token `222:BBB`
- `@infra_noti_bot` → token `333:CCC`

> This is Telegram's own step — anyone running a bot has to do this.

### 1b. Register the 3 bots into the server
Tuan calls the API 3 times (once per bot), declaring: *what the bot is named, its token, which repo it watches, and its secrets*.

After those 3 calls:
- **One server** now holds 3 bots, each tied to one repo.
- Tokens sit safely in the database and are never exposed.

### 1c. Turn on the webhooks
Tuan tells Telegram and GitHub "whenever something happens, call this address":

```
backend repo  → on activity, calls:  noti.mycompany.com/github/webhook/be_noti_bot
mobile repo   → on activity, calls:  noti.mycompany.com/github/webhook/mob_noti_bot
infra repo    → on activity, calls:  noti.mycompany.com/github/webhook/infra_noti_bot
```

→ The admin part is done. From now on, **Tuan doesn't have to touch anything**.

---

## Part 2 — Teams switch it on themselves (no admin involved)

Each team adds its bot to its own group and types a command:

| Group | Typed in the group | Bot's reply |
|---|---|---|
| Backend Team | `/add @be_noti_bot` | ✅ Activated. Will post notifications for `mycompany/backend` |
| Mobile Team | `/add @mob_noti_bot` | ✅ Activated. Will post notifications for `mycompany/mobile` |
| DevOps | `/add @infra_noti_bot` | ✅ Activated. Will post notifications for `mycompany/infra` |

> The nice part: teams are **self-service**. Tuan never edits a config for each group.

Want the **DevOps** group to watch all 3 repos? Just invite all 3 bots into the DevOps group and type:

```
/add @be_noti_bot
/add @mob_noti_bot
/add @infra_noti_bot
```

→ The DevOps group now receives everything. Each bot only responds to its own command — no cross-talk.

---

## Part 3 — A normal working day

### Event 1: A backend dev pushes code
A backend developer pushes 2 commits to `mycompany/backend`.

→ GitHub calls the server via the `be_noti_bot` URL. The server understands: *"this is the Backend bot"*, finds the groups that activated the Backend bot = **[Backend Team, DevOps]**, and posts:

> 🔔 **2 commits** to `mycompany/backend` @ `main` by *quocanh*
> • `abc1234` Fix login API — quocanh
> • `def5678` Add unit test — quocanh

**Mobile Team sees nothing** (this is the backend repo's business).

### Event 2: Mobile opens a Pull Request
→ Only **[Mobile Team, DevOps]** receive:

> 🔀 New PR on `mycompany/mobile`: *"Add payment screen"* by *minh*

### Event 3: Infra ships a new release
→ Only **[DevOps]** receives:

> 🚀 Release `v2.3.0` on `mycompany/infra`

→ Every event automatically reaches **exactly the groups that care**, without spamming the others.

---

## Part 4 — Day-2 operations (where it saves the most effort)

| Situation | The usual way | This app |
|---|---|---|
| **Add a 4th repo** (`mycompany/web`) | Stand up another server/process, add config, build, deploy | Tuan calls **one API**; the server serves the 4th bot instantly |
| **A group wants to stop notifications** | Edit a config file, restart | A member types `/remove @bot`, done |
| **Pause a bot** (repo frozen) | Shut that process down | Set the bot to `enabled: false` via API |
| **Delete a bot entirely** | Tear down a deployment, clean up config | Call the delete API — the server also cleans up that bot's group activations |
| **Change which repo a bot watches** | Edit config, build, redeploy | Call the update API, effective immediately |

**All of this with no downtime, no code changes, no redeploys** — because everything is data in the database, not configuration baked into a build.

---

## The difference in one picture

```
        ┌─────────────────────────────────────────────┐
        │       ONE SERVER (noti.mycompany.com)        │
        │                                              │
        │   be_noti_bot    →  mycompany/backend        │
        │   mob_noti_bot   →  mycompany/mobile         │
        │   infra_noti_bot →  mycompany/infra          │
        │   (+ web_bot, + ... add more anytime)        │
        └─────────────────────────────────────────────┘
              ▲ tokens & config live in the DB
              │ add/remove/edit a bot = one API call, no deploy
```

Compared with the usual way: that many bots = that many separate servers, that many config files, that many deploys.

---

## When this app is overkill

If you only have **1 bot for 1 repo**, a simple standalone bot script is enough — this app is overkill.

It pays off only when you:
- Manage **many bots / many repos** and don't want N deployments
- Want to **add/remove bots and groups while running**, with no downtime and no code changes
- Want **users to self-serve** turning notifications on/off in their own groups, instead of an admin editing config by hand

It solves the problem of **operating many bots from one place**, not the trivial problem of "send a message to several groups".
