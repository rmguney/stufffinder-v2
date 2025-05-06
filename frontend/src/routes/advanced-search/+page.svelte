<script>
  import { onMount } from 'svelte';
  import Post from '$lib/components/post.svelte'; // Import the individual Post component
  import { PUBLIC_API_URL } from '$env/static/public'; // Import the public API URL  
  
  let allPosts = []; // To store all posts fetched initially
  let filteredPosts = []; // To store posts matching the filters
  let filterOptions = {}; // To store unique values for each attribute
  let activeFilters = []; // To store the currently applied filters
  let selectedAttribute = ''; // Currently selected attribute in the dropdown
  let selectedValue = ''; // Currently selected value for the chosen attribute
  let minValue = ''; // For numerical range - minimum value
  let maxValue = ''; // For numerical range - maximum value
  let isLoading = true;
  let error = null;
  let availableAttributes = []; // To store attributes that have at least one non-null value

  const filterableAttributes = [
    'material', 'writtenText', 'mainColor', 'shape',
    'descriptionOfParts', 'location', 'hardness', 'timePeriod', 'smell',
    'taste', 'texture', 'value', 'originOfAcquisition', 'pattern', 'brand',
    'print', 'functionality', 'imageLicenses', 'markings', 'handmade',
    'oneOfAKind', 'width', 'height', 'length', 'weight', 'item_condition'
  ];

  const booleanAttributes = ['handmade', 'oneOfAKind'];
  const numericalAttributes = ['weight', 'value', 'width', 'height', 'length']; // Attributes that should use range selection
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

  // Map between frontend attribute names and backend attribute names
  const attributeMapping = {
    'width': 'sizeX',
    'height': 'sizeY',
    'length': 'sizeZ'
  };

  // Get the backend attribute name for a given frontend attribute name
  function getBackendAttributeName(frontendAttr) {
    return attributeMapping[frontendAttr] || frontendAttr;
  }

  // Get the frontend attribute name for a given backend attribute name
  function getFrontendAttributeName(backendAttr) {
    for (const [frontendAttr, mappedBackendAttr] of Object.entries(attributeMapping)) {
      if (mappedBackendAttr === backendAttr) {
        return frontendAttr;
      }
    }
    return backendAttr;
  }

  function extractFilterOptions() {
    const options = {};
    const attributesWithValues = new Set();
    
    filterableAttributes.forEach(attr => {
      const values = new Set();
      let hasValues = false;
      
      // Use the backend attribute name when accessing post data
      const backendAttr = getBackendAttributeName(attr);
      
      allPosts.forEach(post => {
        if (post.mysteryObject && post.mysteryObject[backendAttr] !== null && post.mysteryObject[backendAttr] !== undefined) {
          values.add(post.mysteryObject[backendAttr]);
          hasValues = true;
        }
      });
      
      // Special handling for boolean and enum
      if (booleanAttributes.includes(attr)) {
        // For boolean attributes, only include if at least one post has a value
        if (hasValues) {
          options[attr] = [true, false];
          attributesWithValues.add(attr);
        }
      } else if (attr === 'item_condition') {
        // For item_condition, only include if at least one post has a value
        if (hasValues) {
          options[attr] = conditionEnumValues;
          attributesWithValues.add(attr);
        }
      } else {
        // For other attributes, only include if there are values
        if (values.size > 0) {
          options[attr] = Array.from(values).sort();
          attributesWithValues.add(attr);
        }
      }
    });
    
    // Update the available attributes list
    availableAttributes = filterableAttributes.filter(attr => attributesWithValues.has(attr));
    filterOptions = options;
  }

  function addFilter() {
    // Avoid adding duplicate filters for the same attribute
    if (activeFilters.some(f => f.attribute === selectedAttribute)) {
      console.warn(`Filter for ${selectedAttribute} already added.`);
      return;
    }

    // Handle numerical attributes with range selection
    if (numericalAttributes.includes(selectedAttribute)) {
      if (minValue !== '' || maxValue !== '') {
        // Convert to numbers for validation and storage
        const min = minValue === '' ? Number.MIN_SAFE_INTEGER : Number(minValue);
        const max = maxValue === '' ? Number.MAX_SAFE_INTEGER : Number(maxValue);
        
        // Validate that min <= max if both are provided
        if (minValue !== '' && maxValue !== '' && Number(minValue) > Number(maxValue)) {
          console.warn('Min value must be less than or equal to max value');
          return;
        }
        
        activeFilters = [...activeFilters, { 
          attribute: selectedAttribute, 
          type: 'range', 
          min, 
          max,
          displayMin: minValue === '' ? '(any)' : minValue,
          displayMax: maxValue === '' ? '(any)' : maxValue
        }];
        
        // Reset selections
        minValue = '';
        maxValue = '';
      }
    } 
    // Handle boolean attributes
    else if (booleanAttributes.includes(selectedAttribute) && selectedValue !== '') {
      const valueToStore = selectedValue === 'true';
      activeFilters = [...activeFilters, { attribute: selectedAttribute, value: valueToStore }];
      selectedValue = '';
    } 
    // Handle other attributes
    else if (selectedValue !== '') {
      activeFilters = [...activeFilters, { attribute: selectedAttribute, value: selectedValue }];
      selectedValue = '';
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
        // Use the backend attribute name when accessing post data
        const backendAttr = getBackendAttributeName(filter.attribute);
        const postValue = post.mysteryObject[backendAttr];
        
        // Handle potential null/undefined values during comparison
        if (postValue === null || postValue === undefined) return false;
        
        // Handle range filters for numerical attributes
        if (filter.type === 'range') {
          const numValue = Number(postValue);
          return numValue >= filter.min && numValue <= filter.max;
        }
        
        // Handle regular value filters (unchanged)
        return String(postValue) === String(filter.value);
      });
    });
  }

  // Reactive statements
  $: currentOptions = filterOptions[selectedAttribute] || [];
  $: if (selectedAttribute) {
    // Reset values when attribute changes
    if (!booleanAttributes.includes(selectedAttribute) && selectedAttribute !== 'item_condition') {
      selectedValue = '';
    }
    if (!numericalAttributes.includes(selectedAttribute)) {
      minValue = '';
      maxValue = '';
    }
  }

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
            {#each availableAttributes as attr}
              <option value={attr}>{attr.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}</option> <!-- Add spaces before caps -->
            {/each}
          </select>
        </div>

        <!-- Value Selection -->
        {#if selectedAttribute}
          <!-- Numerical Range Selection -->
          {#if numericalAttributes.includes(selectedAttribute)}
            <div class="flex flex-col gap-2">
              <label class="block text-sm font-medium text-gray-700">Range:</label>
              <div class="flex items-center gap-2">
                <input 
                  type="number" 
                  bind:value={minValue} 
                  placeholder="Min" 
                  class="mt-1 block w-24 border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                />
                <span class="text-gray-500">to</span>
                <input 
                  type="number" 
                  bind:value={maxValue} 
                  placeholder="Max" 
                  class="mt-1 block w-24 border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                />
                <span class="text-xs text-gray-500">
                  {#if selectedAttribute === 'weight'}
                    (grams)
                  {:else if selectedAttribute === 'value'}
                    ($)
                  {:else if ['width', 'height', 'length'].includes(selectedAttribute)}
                    (cm)
                  {/if}
                </span>
              </div>
              <p class="text-xs text-gray-500">Leave empty for no limit</p>
            </div>
          <!-- Boolean Selection -->
          {:else if booleanAttributes.includes(selectedAttribute)}
            <div>
              <label for="value-select" class="block text-sm font-medium text-gray-700">Value:</label>
              <select id="value-select" bind:value={selectedValue} class="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md">
                <option value="">-- Select Value --</option>
                <option value="true">True</option>
                <option value="false">False</option>
              </select>
            </div>
          <!-- Condition Enum Selection -->
          {:else if selectedAttribute === 'item_condition'}
            <div>
              <label for="value-select" class="block text-sm font-medium text-gray-700">Value:</label>
              <select id="value-select" bind:value={selectedValue} class="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md">
                <option value="">-- Select Condition --</option>
                {#each currentOptions as option}
                  <option value={option}>{option}</option>
                {/each}
              </select>
            </div>
          <!-- Regular Selection with Options -->
          {:else if currentOptions.length > 0}
            <div>
              <label for="value-select" class="block text-sm font-medium text-gray-700">Value:</label>
              <select id="value-select" bind:value={selectedValue} class="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md">
                <option value="">-- Select Value --</option>
                {#each currentOptions as option}
                  <option value={option}>{option}</option>
                {/each}
              </select>
            </div>
          <!-- Text Input for Other Cases -->
          {:else}
            <div>
              <label for="value-input" class="block text-sm font-medium text-gray-700">Value:</label>
              <input 
                id="value-input"
                type="text" 
                bind:value={selectedValue} 
                placeholder="Enter value" 
                class="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
              >
            </div>
          {/if}
        {/if}

        <!-- Add Filter Button -->
        <button 
          on:click={addFilter} 
          disabled={!selectedAttribute || 
            (numericalAttributes.includes(selectedAttribute) ? (minValue === '' && maxValue === '') : selectedValue === '')} 
          class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded disabled:opacity-50"
        >
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
                {#if filter.type === 'range'}
                  {filter.attribute}: {filter.displayMin} to {filter.displayMax}
                  {#if filter.attribute === 'weight'}
                    g
                  {:else if filter.attribute === 'value'}
                    $
                  {:else if ['width', 'height', 'length'].includes(filter.attribute)}
                    cm
                  {/if}
                {:else}
                  {filter.attribute}: {String(filter.value)}
                {/if}
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
