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

    const endpoint = `https://threef.vercel.app/api/register/`;
    const requestOptions = {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        username: registerUsername,
        password: registerPassword,
      }),
    };

    try {
      const response = await fetch(endpoint, requestOptions);
      const contentType = response.headers.get("content-type");
      let data;

      if (contentType && contentType.includes("application/json")) {
        data = await response.json();
      } else {
        const textData = await response.text();
        throw new Error(`Unexpected response format: ${textData}`);
      }

      if (response.ok) {
        console.log("User registered successfully");
        loginBar = false;
        registerUsername = "";
        registerPassword = "";
      } else {
        registerErrors = data;
        console.error("Error registering user:", data);
      }
    } catch (error) {
      console.error("Error registering user:", error);
      registerErrors = { non_field_errors: ["An unexpected error occurred."] };
    }
  };

  let handleLogin = async () => {
    loginErrors = {};

    const endpoint = `https://threef.vercel.app/api/login/`;
    const requestOptions = {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        username: loginUsername,
        password: loginPassword,
      }),
    };

    try {
      const response = await fetch(endpoint, requestOptions);
      const contentType = response.headers.get("content-type");
      let data;

      if (contentType && contentType.includes("application/json")) {
        data = await response.json();
      } else {
        const textData = await response.text();
        throw new Error(`Unexpected response format: ${textData}`);
      }
      if (response.ok) {
        console.log("User logged in successfully");
        activeUser.set(loginUsername);
        loginBar = false;
        loginUsername = "";
        loginPassword = "";
      } else {
        loginErrors = data;
        console.error("Error logging in:", data);
      }
    } catch (error) {
      console.error("Error logging in:", error);
      loginErrors = { non_field_errors: ["An unexpected error occurred."] };
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
              {#if loginErrors.non_field_errors}
                <p class="text-red-500 text-sm">{loginErrors.non_field_errors[0]}</p>
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
              {#if registerErrors.non_field_errors}
                <p class="text-red-500 text-sm">{registerErrors.non_field_errors[0]}</p>
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
