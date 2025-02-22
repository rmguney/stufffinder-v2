<script>
  import { Button } from "$lib/components/ui/button";
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label/index.js";
  import * as Sheet from "$lib/components/ui/sheet";
  import * as Tabs from "$lib/components/ui/tabs/index.js";
  import * as Card from "$lib/components/ui/card/index.js";
  import { activeUser } from '../../userStore'; 

  let loginBar = false;

  let registerUsername = "";
  let registerPassword = "";
  let registerErrors = {};

  let loginUsername = "";
  let loginPassword = "";
  let loginErrors = {};

  let handleRegister = async () => {
    registerErrors = {};

    // Validate required fields
    if (!registerUsername || !registerPassword) {
      registerErrors = { message: "Username and password are required" };
      return;
    }

    const payload = {
      username: registerUsername,
      email: `${registerUsername}@example.com`,
      password: registerPassword
    };

    try {
      const response = await fetch('http://localhost:8080/api/auth/register', {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'Registration failed');
      }

      // Success - try to log in automatically
      await handleLogin(registerUsername, registerPassword);
      loginBar = false;
      registerUsername = "";
      registerPassword = "";
      
    } catch (error) {
      console.error("Error registering user:", error);
      registerErrors = { message: error.message || "Registration failed" };
    }
  };

  let handleLogin = async (username = loginUsername, password = loginPassword) => {
    loginErrors = {};

    // Validate required fields
    if (!username || !password) {
      loginErrors = { message: "Username and password are required" };
      return;
    }

    const payload = {
      username: username,
      password: password
    };

    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'Login failed');
      }

      // Store user data
      activeUser.set(username);
      loginBar = false;
      loginUsername = "";
      loginPassword = "";
      
    } catch (error) {
      console.error("Error logging in:", error);
      loginErrors = { message: error.message || "Login failed" };
    }
  };
</script>

<Button class="lg:px-12 hover:bg-rose-900" on:click={() => (loginBar = !loginBar)}>Sign In</Button>

<Sheet.Root bind:open={loginBar}>
  <Sheet.Overlay />
  <Sheet.Content side="right" class="w-96">
    <Sheet.Header>
      <Sheet.Close />
    </Sheet.Header>
    <div class="pt-8">
      <Tabs.Root value="login" class="w-full p-4">
        <Tabs.List class="grid w-full grid-cols-2">
          <Tabs.Trigger value="login">Login</Tabs.Trigger>
          <Tabs.Trigger value="register">Register</Tabs.Trigger>
        </Tabs.List>
        <Tabs.Content value="login">
          <Card.Root>
            <Card.Header>
              <Card.Title>Login</Card.Title>
              <Card.Description>
                Enter your credentials to access your account.
              </Card.Description>
            </Card.Header>
            <Card.Content class="space-y-2">
              <div class="space-y-1">
                <Label for="login-username">Username</Label>
                <Input id="login-username" type="text" bind:value={loginUsername} />
                {#if loginErrors.username}
                  <p class="text-red-500 text-sm">{loginErrors.username}</p>
                {/if}
              </div>
              <div class="space-y-1">
                <Label for="login-password">Password</Label>
                <Input id="login-password" type="password" bind:value={loginPassword} />
                {#if loginErrors.password}
                  <p class="text-red-500 text-sm">{loginErrors.password}</p>
                {/if}
<!--                 <small><a href="/" class="hover:text-rose-900">Forgot your password?</a></small>
 -->              </div>
              {#if loginErrors.message}
                <p class="text-red-500 text-sm">{loginErrors.message}</p>
              {/if}
            </Card.Content>
            <Card.Footer>
              <Button class="hover:bg-rose-900" on:click={handleLogin}>Login</Button>
            </Card.Footer>
          </Card.Root>
        </Tabs.Content>
        <Tabs.Content value="register">
          <Card.Root>
            <Card.Header>
              <Card.Title>Register</Card.Title>
              <Card.Description>
                Create a new account to join our community!
              </Card.Description>
            </Card.Header>
            <Card.Content class="space-y-2">
              <div class="space-y-1">
                <Label for="register-username">Username</Label>
                <Input id="register-username" type="text" bind:value={registerUsername} />
                {#if registerErrors.username}
                  <p class="text-red-500 text-sm">{registerErrors.username}</p>
                {/if}
              </div>
              <div class="space-y-1">
                <Label for="register-password">Password</Label>
                <Input id="register-password" type="password" bind:value={registerPassword} />
                {#if registerErrors.password}
                  <p class="text-red-500 text-sm">{registerErrors.password}</p>
                {/if}
              </div>
              {#if registerErrors.message}
                <p class="text-red-500 text-sm">{registerErrors.message}</p>
              {/if}
            </Card.Content>
            <Card.Footer>
              <Button class="hover:bg-rose-900" on:click={handleRegister}>Register</Button>
            </Card.Footer>
          </Card.Root>
        </Tabs.Content>
      </Tabs.Root>
    </div>
  </Sheet.Content>
</Sheet.Root>
