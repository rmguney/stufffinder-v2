<script>
  import { onMount } from 'svelte';
  import { get } from '$lib/utils/api'; // Import the get function
  import Post from '$lib/components/post.svelte'; // Import the individual Post component
  import { PUBLIC_API_URL } from '$env/static/public'; // Import the public API URL  
  
  let allPosts = []; // To store all posts fetched initially
  let filteredPosts = []; // To store posts matching the filters
  let filterOptions = {}; // To store unique values for each attribute
  let activeFilters = []; // To store the currently applied filters
  let selectedAttribute = ''; // Currently selected attribute in the dropdown
  let selectedValue = ''; // Currently selected value for the chosen attribute
  let isLoading = true;
  let error = null;

  const filterableAttributes = [
    'description', 'material', 'writtenText', 'mainColor', 'shape',
    'descriptionOfParts', 'location', 'hardness', 'timePeriod', 'smell',
    'taste', 'texture', 'value', 'originOfAcquisition', 'pattern', 'brand',
    'print', 'functionality', 'imageLicenses', 'markings', 'handmade',
    'oneOfAKind', 'sizeX', 'sizeY', 'sizeZ', 'weight', 'item_condition'
  ];

  const booleanAttributes = ['handmade', 'oneOfAKind'];
  const conditionEnumValues = ['NEW', 'LIKE_NEW', 'USED', 'DAMAGED', 'ANTIQUE']; // From MysteryObject.java

  onMount(async () => {
    isLoading = true;
    error = null;
    try {
      // Fetch data from /api/posts/all/details/no-media using the imported get function
      allPosts = await fetch(`${PUBLIC_API_URL}/api/posts/all/details/no-media`);
      allPosts = await allPosts.json();

      if (!Array.isArray(allPosts)) {
          console.error("Fetched data is not an array:", allPosts);
          throw new Error('Received invalid data format from server.');
      }

      extractFilterOptions();
      filteredPosts = [...allPosts]; // Initially show all posts
    } catch (err) {
      error = err.message;
      console.error("Error fetching posts:", err);
    } finally {
      isLoading = false;
    }
  });

  function extractFilterOptions() {
    const options = {};
    filterableAttributes.forEach(attr => {
      const values = new Set();
      allPosts.forEach(post => {
        if (post.mysteryObject && post.mysteryObject[attr] !== null && post.mysteryObject[attr] !== undefined) {
          values.add(post.mysteryObject[attr]);
        }
      });
       // Special handling for boolean and enum
       if (booleanAttributes.includes(attr)) {
         options[attr] = [true, false];
       } else if (attr === 'item_condition') {
         options[attr] = conditionEnumValues;
       } else {
         options[attr] = Array.from(values).sort();
       }
    });
    filterOptions = options;
  }

  function addFilter() {
    if (selectedAttribute && selectedValue !== '') {
      // Avoid adding duplicate filters for the same attribute
      if (!activeFilters.some(f => f.attribute === selectedAttribute)) {
         // Convert string 'true'/'false' back to boolean if needed
         let valueToStore = selectedValue;
         if (booleanAttributes.includes(selectedAttribute)) {
             valueToStore = selectedValue === 'true';
         }
         activeFilters = [...activeFilters, { attribute: selectedAttribute, value: valueToStore }];
         // Reset selections
         // selectedAttribute = ''; // Keep attribute selected? Maybe allow multiple values? For now, reset.
         selectedValue = '';
      } else {
          // Optionally notify user that filter for this attribute already exists
          console.warn(`Filter for ${selectedAttribute} already added.`);
      }
    }
  }

  function removeFilter(attributeToRemove) {
    activeFilters = activeFilters.filter(f => f.attribute !== attributeToRemove);
  }

  function performSearch() {
    if (activeFilters.length === 0) {
      filteredPosts = [...allPosts];
      return;
    }

    filteredPosts = allPosts.filter(post => {
      if (!post.mysteryObject) return false; // Skip posts without mystery objects

      return activeFilters.every(filter => {
        const postValue = post.mysteryObject[filter.attribute];
        // Handle potential null/undefined values during comparison
        if (postValue === null || postValue === undefined) return false;
        return String(postValue) === String(filter.value); // Compare as strings for simplicity for now
      });
    });
  }

  // Reactive statement to update value options when attribute changes
  $: currentOptions = filterOptions[selectedAttribute] || [];
  $: if (selectedAttribute && !booleanAttributes.includes(selectedAttribute) && selectedAttribute !== 'item_condition') selectedValue = ''; // Reset value when attribute changes, except for boolean/enum

</script>

<svelte:head>
  <title>Advanced Search</title>
</svelte:head>

<div class="container mx-auto p-4">
  <h1 class="text-2xl font-bold mb-4">Advanced Post Search</h1>

  {#if isLoading}
    <p>Loading posts...</p>
  {:else if error}
    <p class="text-red-500">Error: {error}</p>
  {:else}
    <!-- Filter Builder -->
    <div class="filter-builder bg-gray-100 p-4 rounded mb-6">
      <h2 class="text-xl mb-2">Build Your Filter</h2>
      <div class="flex flex-wrap gap-4 items-end">
        <!-- Attribute Selection -->
        <div>
          <label for="attribute-select" class="block text-sm font-medium text-gray-700">Attribute:</label>
          <select id="attribute-select" bind:value={selectedAttribute} class="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md">
            <option value="">-- Select Attribute --</option>
            {#each filterableAttributes as attr}
              <option value={attr}>{attr.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}</option> <!-- Add spaces before caps -->
            {/each}
          </select>
        </div>

        <!-- Value Selection -->
        {#if selectedAttribute}
          <div>
            <label for="value-select" class="block text-sm font-medium text-gray-700">Value:</label>
            {#if booleanAttributes.includes(selectedAttribute)}
              <select id="value-select" bind:value={selectedValue} class="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md">
                 <option value="">-- Select Value --</option>
                 <option value="true">True</option>
                 <option value="false">False</option>
              </select>
            {:else if selectedAttribute === 'item_condition'}
               <select id="value-select" bind:value={selectedValue} class="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md">
                 <option value="">-- Select Condition --</option>
                 {#each currentOptions as option}
                   <option value={option}>{option}</option>
                 {/each}
               </select>
            {:else if currentOptions.length > 0}
              <select id="value-select" bind:value={selectedValue} class="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md">
                <option value="">-- Select Value --</option>
                {#each currentOptions as option}
                  <option value={option}>{option}</option>
                {/each}
              </select>
            {:else}
               <input type="text" bind:value={selectedValue} placeholder="Enter value" class="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
               <!-- Consider type="number" for numeric fields if needed -->
            {/if}
          </div>
        {/if}

        <!-- Add Filter Button -->
        <button on:click={addFilter} disabled={!selectedAttribute || selectedValue === ''} class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded disabled:opacity-50">
          Add Filter
        </button>
      </div>

      <!-- Active Filters Display -->
      {#if activeFilters.length > 0}
        <div class="mt-4">
          <h3 class="text-lg mb-1">Active Filters:</h3>
          <ul class="flex flex-wrap gap-2">
            {#each activeFilters as filter}
              <li class="bg-gray-300 rounded px-2 py-1 text-sm flex items-center">
                {filter.attribute}: {String(filter.value)}
                <button on:click={() => removeFilter(filter.attribute)} class="ml-2 text-red-500 hover:text-red-700 font-bold">×</button>
              </li>
            {/each}
          </ul>
        </div>
      {/if}
    </div>

    <!-- Search Execution -->
    <div class="mb-6">
      <button on:click={performSearch} class="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded w-full sm:w-auto">
        Search Posts
      </button>
    </div>

    <!-- Search Results -->
    <!-- Search Results -->
    <div class="search-results">
      <h2 class="text-xl mb-2">Results ({filteredPosts.length})</h2>
      {#if filteredPosts.length > 0}
         <!-- Iterate and render individual Post components -->
         <div class="flex flex-col lg:flex-wrap lg:flex-row justify-center gap-4 lg:gap-6">
            {#each filteredPosts as post (post.id)}
              <div class="w-full lg:w-[calc(33.333%-1rem)]">
                 <a href={`/thread/${post.id}`}>
                    <Post
                       id={post.id}
                       title={post.title}
                       description={post.description || ""}
                       tags={post.tags || []}
                       imageSrc={post.mysteryObject?.imageUrl || ''}
                       mediaFiles={post.mediaFiles || []}
                       postedBy={post.author}
                       createdAt={post.createdAt}
                       updatedAt={post.updatedAt}
                       upvotes={post.upvotes || 0}
                       downvotes={post.downvotes || 0}
                       commentCount={post.commentCount || 0}
                       userUpvoted={post.userUpvoted || false}
                       userDownvoted={post.userDownvoted || false}
                       solved={post.solved}
                       mysteryObject={post.mysteryObject || null}
                       variant="thumb"
                    />
                 </a>
              </div>
            {/each}
         </div>
      {:else}
        <p>No posts match the current filters or no posts loaded.</p>
      {/if}
    </div>
  {/if}
</div>
