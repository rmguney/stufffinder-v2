<script>
  import { Button } from "$lib/components/ui/button";
  import { Input } from "$lib/components/ui/input";
  import { Separator } from "$lib/components/ui/separator";
  import { toggleMode } from "mode-watcher";
  import Sun from "lucide-svelte/icons/sun";
  import Moon from "lucide-svelte/icons/moon";
  import * as Sheet from "$lib/components/ui/sheet";
  import Login from "$lib/components/login.svelte";
  import { activeUser } from "../../userStore";
  import * as Popover from "$lib/components/ui/popover/index.js";
  import { goto } from "$app/navigation";

  let navbar = false;
  let loginBar = false;
  let searchQuery = ""; // Bind to the search input
  let searchResults = []; // Holds filtered search results
  let allThreads = []; // Cache for all threads
  let loading = false; // Loading state
  let showDropdown = false; // Controls the visibility of the dropdown
  let highlightedIndex = -1; // For keyboard navigation
  let selectedFields = [
    "title",
    "description",
    "labels",
    "material",
    "color",
  ]; // Default fields to search in

  const availableFields = [
    { value: "title", label: "Title of the Post" },
    { value: "description", label: "Description" },
    { value: "labels", label: "Wikidata Tags" },
    { value: "material", label: "Material" },
    { value: "color", label: "Color" },
    { value: "shape", label: "Shape" },
    { value: "texture", label: "Texture or Markings" },
    { value: "smell", label: "Smell/Taste" },
    { value: "functionality", label: "Functionality" },
    { value: "period", label: "Time Period" },
    { value: "location", label: "Location" },
    { value: "size", label: "Size & Dimensions" },
    { value: "weight", label: "Weight" },
    { value: "postedBy", label: "Posted By User" },
  ]; // All available fields

  // Function to fetch all threads initially
  async function fetchAllThreads() {
    loading = true;
    try {
      const response = await fetch("https://threef.vercel.app/api/thread/");
      if (response.ok) {
        allThreads = await response.json();
        searchResults = allThreads; // Initialize results with all threads
      } else {
        console.error("Failed to fetch threads");
      }
    } catch (error) {
      console.error("Error fetching threads:", error);
    } finally {
      loading = false;
    }
  }

  let debounceTimeout;

  function filterThreads() {
  clearTimeout(debounceTimeout);
  debounceTimeout = setTimeout(() => {
    const terms = searchQuery.trim().toLowerCase().split(/\s+/); // Normalize the search query to lowercase
    if (terms.length < 1 || terms[0].length < 3) {
      searchResults = [];
      showDropdown = false;
      return;
    }

    searchResults = allThreads
      .map((thread) => {
        const content = selectedFields
          .map((field) => {
            const value = thread[field];
            if (Array.isArray(value)) {
              // Join array values, e.g., for "labels", and normalize to lowercase
              return value.map((item) => item.toLowerCase()).join(" ");
            }
            // Handle non-array fields safely and normalize to lowercase
            return value ? value.toString().toLowerCase() : "";
          })
          .join(" ");

        // Count matches by checking if content includes each search term
        const matchCount = terms.reduce(
          (count, term) => (content.includes(term) ? count + 1 : count),
          0
        );
        return { thread, matchCount };
      })
      .filter((item) => item.matchCount > 0) // Only include threads with matches
      .sort((a, b) => b.matchCount - a.matchCount) // Sort by match count
      .map((item) => item.thread);

    showDropdown = searchResults.length > 0;
  }, 300); // Adjust debounce delay as needed
}

  // Highlight matching terms
  function highlightMatches(text, query) {
    const terms = query.trim().toLowerCase().split(/\s+/);
    return text.replace(new RegExp(`(${terms.join("|")})`, "gi"), "<mark>$1</mark>");
  }

  // Toggle selected fields for filtering
  function toggleField(fieldValue) {
    if (selectedFields.includes(fieldValue)) {
      selectedFields = selectedFields.filter((f) => f !== fieldValue);
    } else {
      selectedFields = [...selectedFields, fieldValue];
    }
    filterThreads();
  }

  function toggleAllFields(selectAll) {
    if (selectAll) {
      selectedFields = availableFields.map(field => field.value);
    } else {
      selectedFields = [];
    }
    filterThreads();
  }

  // Keyboard navigation
  function handleKeydown(event) {
    if (!showDropdown) return;

    if (event.key === "ArrowDown") {
      highlightedIndex = (highlightedIndex + 1) % searchResults.length;
    } else if (event.key === "ArrowUp") {
      highlightedIndex = (highlightedIndex - 1 + searchResults.length) % searchResults.length;
    } else if (event.key === "Enter") {
      if (highlightedIndex >= 0 && highlightedIndex < searchResults.length) {
        window.location.href = `/thread/${searchResults[highlightedIndex].id}`;
      }
    }
  }

  // Fetch threads on component mount
  fetchAllThreads();

  let menuOpen = false;

  function handleLogout() {
      activeUser.set(null);
      goto("/");
  }

  async function getNotifications() {
      try {
          // First get user ID from username
          const userResponse = await fetch(`http://localhost:8080/api/auth/${$activeUser}`);
          if (!userResponse.ok) throw new Error('Failed to fetch user');
          const userData = await userResponse.json();
          
          // Then fetch notifications for that user
          const notifResponse = await fetch(`http://localhost:8080/api/notifications/${userData.id}`);
          if (!notifResponse.ok) throw new Error('Failed to fetch notifications');
          const notifications = await notifResponse.json();
          
          // Handle notifications as needed
          console.log(notifications);
          return notifications;
      } catch (error) {
          console.error('Error fetching notifications:', error);
          return [];
      }
  }
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
  class="sticky top-0 flex justify-between items-center p-4 gap-2 w-full shadow-md bg-white dark:bg-black z-50"
  on:keydown={handleKeydown}
>
  <button on:click={() => (navbar = !navbar)} variant="outline">
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6 hover:text-rose-900 lg:hidden">
      <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 9h16.5m-16.5 6.75h16.5" />
    </svg>
    <h1 class="text-2xl font-bold hover:text-rose-900 hidden lg:block">= Stufffinder</h1>
  </button>

  <!-- Search Bar -->
  <div class="flex items-center gap-2 relative" role="search">
    <label for="search-bar" class="sr-only">Search</label>
    <Input
      id="search-bar"
      type="search"
      placeholder="Search stuff"
      bind:value={searchQuery}
      class="lg:w-96"
      on:input={filterThreads}
      on:focus={() => (showDropdown = searchQuery.trim().length >= 3)}
      aria-autocomplete="list"
      aria-expanded={showDropdown ? "true" : "false"}
      aria-owns="search-results"
      aria-controls="search-results"
    />

    <Popover.Root>
      <Popover.Trigger>
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6 hover:text-rose-900">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 6h9.75M10.5 6a1.5 1.5 0 1 1-3 0m3 0a1.5 1.5 0 1 0-3 0M3.75 6H7.5m3 12h9.75m-9.75 0a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m-3.75 0H7.5m9-6h3.75m-3.75 0a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m-9.75 0h9.75" />
      </svg>
      </Popover.Trigger>
      <Popover.Content>
        <div class="px-4">
          <h4>Search Fields</h4>
          <Separator class="my-2"/>
          <div class="flex gap-2 mb-2">
            <Button 
            class="hover:bg-rose-900"
              variant="outline" 
              size="sm" 
              on:click={() => toggleAllFields(true)}
            >
              Check All
            </Button>
            <Button 
            class="hover:bg-rose-900"
              variant="outline" 
              size="sm" 
              on:click={() => toggleAllFields(false)}
            >
              Uncheck All
            </Button>
          </div>
          <ul>
            {#each availableFields as field}
              <li>
                <label class="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={selectedFields.includes(field.value)}
                    on:change={() => toggleField(field.value)}
                  />
                  {field.label}
                </label>
              </li>
            {/each}
          </ul>
        </div>
      </Popover.Content>
    </Popover.Root>  

    {#if showDropdown}
      <ul
        id="search-results"
        class="absolute top-full w-full bg-white dark:bg-black shadow-md rounded-md overflow-hidden z-50"
        role="listbox"
        aria-label="Search Results"
      >
        {#each searchResults as result, index (result.id)}
          <!-- svelte-ignore a11y-role-has-required-aria-props -->
          <li
            role="option"
            class="p-2"
            class:hover={index === highlightedIndex ? "bg-gray-200 dark:bg-gray-700" : ""}
          >
            <button
              class="w-full text-left hover:bg-rose-900 cursor-pointer"
              on:click={() => (showDropdown = false)}
            >
              <a href={`/thread/${result.id}`} class="block">{@html highlightMatches(result.title, searchQuery)}</a>
            </button>
          </li>
        {/each}
        {#if searchResults.length === 0 && !loading}
          <!-- svelte-ignore a11y-role-has-required-aria-props -->
          <li role="option" class="p-2 text-gray-500 dark:text-gray-300">No results found</li>
        {/if}
      </ul>
    {/if}
  </div>

  <div class="flex items-center gap-2">
    {#if $activeUser}
      <span class="lg:mr-0 -mr-3">Welcome <a href={`/user/${$activeUser}`}
        class="text-rose-900 hover:underline font-bold">{$activeUser}!</a></span>
      <Button class="lg:px-12 transition-all hover:bg-rose-900" on:click={handleLogout}>
        Logout
      </Button>
    {:else}
      <Login />
    {/if}

    <Button
      on:click={toggleMode}
      variant="outline"
      size="icon"
      class="bg-black dark:bg-white text-white dark:text-black hover:bg-rose-900 dark:hover:bg-rose-900 hover:text-white transition-colors duration-300"
    >
      <Sun class="h-[1.2rem] w-[1.2rem] rotate-0 scale-100 transition-transform duration-300 dark:-rotate-90 dark:scale-0" />
      <Moon class="absolute h-[1.2rem] w-[1.2rem] rotate-90 scale-0 transition-transform duration-300 dark:rotate-0 dark:scale-100" />
    </Button>
  </div>
</div>

<Sheet.Root bind:open={navbar}>
  <Sheet.Overlay />
  <Sheet.Content side="left" class="w-48">
    <Sheet.Header>
      <Sheet.Title>
        <h3 class="text-8xl mt-8 mb-4 font-bold text-center">3F</h3>
      </Sheet.Title>
      <Sheet.Close />
    </Sheet.Header>
    <Separator />

    <nav class="mt-8 text-center">
      <ul class="space-y-8">
        <li><a href="/" class="block hover:text-rose-900 text-lg font-semibold" on:click={() => (navbar = false)}>Home</a></li>
        {#if $activeUser}
        <li><a href="/create" class="block hover:text-rose-900 text-lg font-semibold" on:click={() => (navbar = false)}>Create</a></li>
        {/if}
        <li><a href="/about" class="block hover:text-rose-900 text-lg font-semibold" on:click={() => (navbar = false)}>About</a></li>
      </ul>
    </nav>
  </Sheet.Content>
</Sheet.Root>

<Sheet.Root bind:open={loginBar}>
  <Sheet.Overlay />
  <Sheet.Content side="right" class="w-96">
    <Sheet.Header>
      <Sheet.Close />
    </Sheet.Header>
    <Login />
  </Sheet.Content>
</Sheet.Root>
