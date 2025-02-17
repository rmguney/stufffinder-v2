<script>
  import { onMount } from 'svelte';
  import { writable } from 'svelte/store';
  import { Button } from "$lib/components/ui/button";

  let searchTerm = '';
  let searchResults = writable([]); // Stores search results from Wikidata
  let selectedItems = writable([]); // Stores selected tags with format { id, label, description }
  let isLoading = writable(false); // Tracks loading state
  let showResults = writable(false); // Toggles search results dropdown visibility

  // Function to search Wikidata
  const searchWikidata = async () => {
    if (!searchTerm) {
      searchResults.set([]);
      showResults.set(false);
      return;
    }

    isLoading.set(true);
    showResults.set(true);

    try {
      const response = await fetch(`https://www.wikidata.org/w/api.php?action=wbsearchentities&search=${encodeURIComponent(searchTerm)}&language=en&format=json&origin=*`);
      const data = await response.json();
      // Map results to desired format
      const formattedResults = data.search.map(item => ({
        id: item.id,
        label: item.label,
        description: item.description || "No description available"
      }));
      searchResults.set(formattedResults);
    } catch (error) {
      console.error('Search failed', error);
      searchResults.set([]);
    } finally {
      isLoading.set(false);
    }
  };

  // Add a tag to the selected list
  const selectItem = (item) => {
    selectedItems.update((items) => {
      if (!items.find((i) => i.id === item.id)) {
        return [...items, item];
      }
      return items;
    });
    showResults.set(false);
  };

  // Remove a tag from the selected list
  const removeSelectedItem = (itemId) => {
    selectedItems.update((items) => items.filter((i) => i.id !== itemId));
  };

  // Hide search results dropdown when clicking outside
  onMount(() => {
    const handleClickOutside = (event) => {
      const searchContainer = document.querySelector('.search-container');
      if (!searchContainer.contains(event.target)) {
        showResults.set(false);
      }
    };
    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  });

  // Reflect changes in `selectedItems` to `tags` and 'labels'
  $: tags = $selectedItems;
  $: labels = $selectedItems;

  // Passed down to the parent component
  export let tags;
  export let labels; 
</script>

<div class="w-full mx-auto relative search-container text-sm">
  <!-- Search Input -->
  <input
    type="text"
    placeholder="Add relevant tags"
    bind:value={searchTerm}
    on:input={searchWikidata}
    class="w-full p-3 border border-gray-300 rounded-md bg-white dark:border-gray-600 dark:bg-neutral-950 text-sm text-black dark:text-white"
  />

  <!-- Loading Indicator -->
  {#if $isLoading}
    <p class="mt-2 text-black dark:text-white">Loading...</p>
  {/if}

  <!-- Search Results -->
  {#if $showResults && $searchResults.length > 0}
    <ul class="list-none p-0 mt-2 border bg-white dark:bg-neutral-950 rounded-md absolute w-full max-h-60 overflow-y-auto shadow-lg z-10">
      {#each $searchResults as result}
        <li class="mt-2">
          <button 
            on:click={() => selectItem(result)} 
            class="w-full text-left p-3 hover:bg-rose-900 hover:text-white text-black dark:text-white"
          >
            {result.label}: {result.description}
          </button>
        </li>
      {/each}
    </ul>
  {/if}

  <!-- Selected Items -->
  <div class="selected-items mt-4">
    <h3 class="text-md font-semibold text-black dark:text-white">Added Tags:</h3>
    <ul class="list-none p-0 mt-2">
      {#each $selectedItems as selectedItem}
        <li class="mt-2 flex items-center">
          <a 
            href={`https://www.wikidata.org/wiki/${selectedItem.id}`} 
            target="_blank" 
            rel="noopener noreferrer"
            class="underline text-black dark:text-white hover:text-rose-700 dark:hover:text-rose-900"
          >
            {selectedItem.label}: {selectedItem.description}
          </a>
          <Button 
            on:click={() => removeSelectedItem(selectedItem.id)} 
            class="ml-4 bg-neutral-950 dark:bg-white text-white dark:text-black hover:bg-rose-900 hover:text-white hover:dark:bg-rose-900 transition-colors duration-300"
          >
            Remove
          </Button>
        </li>
      {/each}
    </ul>
  </div>
</div>
