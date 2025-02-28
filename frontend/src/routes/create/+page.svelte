<script>
    import * as Card from "$lib/components/ui/card/index.js";
    import { Button } from "$lib/components/ui/button";
    import Query from '$lib/components/query.svelte';
    import { threadStore } from "../../threadStore";
    import { goto } from '$app/navigation';
    import { activeUser } from '../../userStore';
    import { Input } from "$lib/components/ui/input/index.js";
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import { PUBLIC_API_URL } from "$env/static/public";

    let imageFile = null;
    let title = '';
    let tags = [];
    let labels = [];
    let postedBy;
    let description = '';
    let material = '';
    // Replace size with dimensional measurements
    let sizeX = '';
    let sizeY = '';
    let sizeZ = '';
    let shape = '';
    let color = '';
    let texture = '';
    let weight = '';
    let smell = '';
    let taste = '';
    let marking = '';
    let functionality = '';
    let period = '';
    let location = '';
    let anonymous = false; 
    let resolved = false;
    // Additional fields from MysteryObject entity
    let writtenText = '';
    let descriptionOfParts = '';
    let hardness = '';
    let value = '';
    let originOfAcquisition = '';
    let pattern = '';
    let brand = '';
    let print = '';
    let imageLicenses = '';
    let handmade = false;
    let oneOfAKind = false;
    let itemCondition = '';
    
    let errors = {
        title: '',
        image: '',
        description: ''
    };

    $: postedBy = $activeUser;

    async function fetchLabelForQcode(qcode) {
        const wikidataApiUrl = `https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${qcode}&format=json&languages=en&origin=*`;
        try {
            const response = await fetch(wikidataApiUrl);
            const data = await response.json();
            const label = data.entities[qcode]?.labels?.en?.value;
            if (!label) throw new Error(`Label not found for qcode: ${qcode}`);
            return label;
        } catch (error) {
            console.error('Error fetching label:', error);
            return null;
        }
    }

    async function enrichTagsWithLabels(tags) {
        const enrichedTags = [];
        for (const tag of tags) {
            const label = await fetchLabelForQcode(tag.id);
            if (label) {
                enrichedTags.push({ id: tag.id, label });
            }
        }
        return enrichedTags;
    }
    
    let handlePost = async () => {
        // Reset error state
        errors = {
            title: '',
            image: '',
            description: ''
        };
        
        // Validate required fields
        let hasErrors = false;
        
        if (!title.trim()) {
            errors.title = 'Title is required';
            hasErrors = true;
        }
        
        if (!imageFile) {
            errors.image = 'Image is required';
            hasErrors = true;
        }
        
        if (!description.trim()) {
            errors.description = 'Description is required';
            hasErrors = true;
        }
        
        if (hasErrors) {
            return; // Don't proceed if validation fails
        }
    
        try {
            const enrichedTags = await enrichTagsWithLabels(tags);
            
            // Create mysteryObject matching backend entity structure
            const mysteryObject = {
                description,
                color,
                shape,
                location,
                smell,
                taste: taste || smell,
                texture,
                functionality,
                markings: marking,
                handmade: handmade || false,
                oneOfAKind: oneOfAKind || false,
                weight: parseFloat(weight) || null,
                timePeriod: period,
                writtenText,
                descriptionOfParts,
                hardness,
                value: parseFloat(value) || null,
                originOfAcquisition,
                pattern,
                brand,
                print,
                imageLicenses,
                sizeX: parseFloat(sizeX) || null,
                sizeY: parseFloat(sizeY) || null,
                sizeZ: parseFloat(sizeZ) || null,
                item_condition: itemCondition || null
            };
            
            // Create FormData for multipart request
            const formData = new FormData();
            formData.append('title', title);
            formData.append('content', description);
            
            // Add tags
            if (tags && tags.length > 0) {
                const tagIds = tags.map(tag => tag.id);
                formData.append('tags', new Blob([JSON.stringify(tagIds)], {
                    type: 'application/json'
                }));
            }
            
            // Add mystery object as JSON string
            formData.append('mysteryObject', new Blob([JSON.stringify(mysteryObject)], {
                type: 'application/json'
            }));
    
            // Create post first
            const response = await fetch(`${PUBLIC_API_URL}/api/posts/create`, {
                method: 'POST',
                body: formData,
                credentials: 'include'
            });
    
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to create post');
            }
            
            const responseData = await response.json();
            
            // If we have an image, upload it separately
            if (imageFile) {
                const imageFormData = new FormData();
                imageFormData.append('image', imageFile);
                
                const imageResponse = await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${responseData.mysteryObjectId}/upload-image`, {
                    method: 'POST',
                    body: imageFormData,
                    credentials: 'include'
                });
    
                if (!imageResponse.ok) {
                    console.error('Failed to upload image');
                    // Don't throw error here, as the post was created successfully
                }
            }
    
            threadStore.update(prev => [...prev, responseData]);
            goto('/');
        } catch (error) {
            console.error('Error creating post:', error);
            alert(`Failed to create post: ${error.message}`);
        }
    };

    // Handle file input change
    function handleFileChange(event) {
        const file = event.target.files[0];
        if (file) {
            imageFile = file;
        }
    }
</script>

<div class="flex justify-center p-6 lg:py-10 bg-change dark:bg-dark shifting">
    <form class="w-full lg:w-2/3" on:submit|preventDefault={handlePost}>
        <Card.Root class="bg-opacity-90">
            <Card.Title class="p-4 text-2xl mt-6 text-center">
                Let's help you post new stuff
                <small class="block text-sm mt-2 font-semibold">Fill in the details about your object, you can leave any blank if you are not sure, but try to be as precise as possible</small>
            </Card.Title>
        <div class="bg-opacity-95 rounded-lg shadow-lg p-6">

            <!-- Title - Keep as is -->
            <div class="mb-4">
                <label for="title" class="block text-sm font-medium mb-2">Title*</label>
                <Textarea id="title" class="w-full p-2 border rounded dark:border-gray-600 h-auto" bind:value={title} placeholder="This is what people will see on their homepage so try to make it interesting" />
                {#if errors.title}
                    <p class="text-red-500 text-sm mt-1">{errors.title}</p>
                {/if}
            </div>

            <!-- Object Details -->
            <div class="mb-4 grid grid-cols-1 lg:grid-cols-2 gap-4">
                <!-- Keep existing fields -->
                <div>
                    <label for="material" class="block text-sm font-medium mb-2">Material</label>
                    <Input id="material" class="w-full p-2 border rounded dark:border-gray-600" bind:value={material} placeholder="E.g., wood, metal, plastic, fabric" />
                </div>
                <div>
                    <label for="shape" class="block text-sm font-medium mb-2">Shape</label>
                    <Input id="shape" class="w-full p-2 border rounded dark:border-gray-600" bind:value={shape} placeholder="E.g., round, square or something more elaborate" />
                </div>
                <div>
                    <label for="color" class="block text-sm font-medium mb-2">Color</label>
                    <Input id="color" class="w-full p-2 border rounded dark:border-gray-600" bind:value={color} placeholder="E.g., red, blue, yellow, transparent" />
                </div>
                <div>
                    <label for="texture" class="block text-sm font-medium mb-2">Texture</label>
                    <Input id="texture" class="w-full p-2 border rounded dark:border-gray-600" bind:value={texture} placeholder="E.g., smooth, rough, bumpy" />
                </div>
                <div>
                    <label for="marking" class="block text-sm font-medium mb-2">Markings</label>
                    <Input id="marking" class="w-full p-2 border rounded dark:border-gray-600" bind:value={marking} placeholder="Does it have logo, text, engravings etc?" />
                </div>
                <div>
                    <label for="smell" class="block text-sm font-medium mb-2">Smell</label>
                    <Input id="smell" class="w-full p-2 border rounded dark:border-gray-600" bind:value={smell} placeholder="E.g., sweet, odorless, pungent" />
                </div>
                <div>
                    <label for="taste" class="block text-sm font-medium mb-2">Taste</label>
                    <Input id="taste" class="w-full p-2 border rounded dark:border-gray-600" bind:value={taste} placeholder="Don't lick weird stuff" />
                </div>
                <div>
                    <label for="functionality" class="block text-sm font-medium mb-2">Functionality</label>
                    <Input id="functionality" class="w-full p-2 border rounded dark:border-gray-600" bind:value={functionality} placeholder="E.g., cutting, writing, art" />
                </div>
                <div>
                    <label for="period" class="block text-sm font-medium mb-2">Time Period</label>
                    <Input id="period" class="w-full p-2 border rounded dark:border-gray-600" bind:value={period} placeholder="E.g., 1800s, 1900s, 2000s" />
                </div>
                <div>
                    <label for="location" class="block text-sm font-medium mb-2">Location</label>
                    <Input id="location" class="w-full p-2 border rounded dark:border-gray-600" bind:value={location} placeholder="Where is it typically found? E.g., Europe, Asia" />
                </div>
                <div>
                    <label for="brand" class="block text-sm font-medium mb-2">Brand</label>
                    <Input id="brand" class="w-full p-2 border rounded dark:border-gray-600" bind:value={brand} placeholder="Brand name if applicable" />
                </div>
                
                <!-- Add missing fields -->
                <div>
                    <label for="writtenText" class="block text-sm font-medium mb-2">Written Text</label>
                    <Input id="writtenText" class="w-full p-2 border rounded dark:border-gray-600" bind:value={writtenText} placeholder="Any writing on the object" />
                </div>
                <div>
                    <label for="descriptionOfParts" class="block text-sm font-medium mb-2">Parts Description</label>
                    <Input id="descriptionOfParts" class="w-full p-2 border rounded dark:border-gray-600" bind:value={descriptionOfParts} placeholder="Description of individual parts" />
                </div>
                <div>
                    <label for="hardness" class="block text-sm font-medium mb-2">Hardness</label>
                    <Input id="hardness" class="w-full p-2 border rounded dark:border-gray-600" bind:value={hardness} placeholder="E.g., soft, hard, flexible" />
                </div>
                <div>
                    <label for="value" class="block text-sm font-medium mb-2">Value ($)</label>
                    <Input type="number" id="value" class="w-full p-2 border rounded dark:border-gray-600" bind:value={value} placeholder="Estimated value in dollars" />
                </div>
                <div>
                    <label for="originOfAcquisition" class="block text-sm font-medium mb-2">Origin of Acquisition</label>
                    <Input id="originOfAcquisition" class="w-full p-2 border rounded dark:border-gray-600" bind:value={originOfAcquisition} placeholder="Where/how you got it" />
                </div>
                <div>
                    <label for="pattern" class="block text-sm font-medium mb-2">Pattern</label>
                    <Input id="pattern" class="w-full p-2 border rounded dark:border-gray-600" bind:value={pattern} placeholder="E.g., striped, checkered, floral" />
                </div>
                <div>
                    <label for="print" class="block text-sm font-medium mb-2">Print</label>
                    <Input id="print" class="w-full p-2 border rounded dark:border-gray-600" bind:value={print} placeholder="Any printed design or imagery" />
                </div>
                <div>
                    <label for="imageLicenses" class="block text-sm font-medium mb-2">Image Licenses</label>
                    <Input id="imageLicenses" class="w-full p-2 border rounded dark:border-gray-600" bind:value={imageLicenses} placeholder="Licenses for included images" />
                </div>
                <div>
                    <label for="condition" class="block text-sm font-medium mb-2">Condition</label>
                    <select id="condition" bind:value={itemCondition} class="w-full p-2 rounded border dark:border-gray-600 dark:bg-neutral-950 text-sm">
                        <option value="" selected>Select condition (optional)</option>
                        <option value="NEW">New</option>
                        <option value="LIKE_NEW">Like New</option>
                        <option value="USED">Used</option>
                        <option value="DAMAGED">Damaged</option>
                        <option value="ANTIQUE">Antique</option>
                    </select>
                </div>
            </div>

            <!-- Replace size dropdown with dimensional measurements -->
            <div class="mb-4">
                <div id="dimensions-label" class="block text-sm font-medium mb-2">Dimensions (cm)</div>
                <div class="grid grid-cols-3 gap-2" role="group" aria-labelledby="dimensions-label">
                    <div>
                        <label for="sizeX" class="block text-xs font-medium mb-1">Length</label>
                        <Input type="number" id="sizeX" class="w-full p-2 border rounded dark:border-gray-600" bind:value={sizeX} placeholder="Length (cm)" />
                    </div>
                    <div>
                        <label for="sizeY" class="block text-xs font-medium mb-1">Width</label>
                        <Input type="number" id="sizeY" class="w-full p-2 border rounded dark:border-gray-600" bind:value={sizeY} placeholder="Width (cm)" />
                    </div>
                    <div>
                        <label for="sizeZ" class="block text-xs font-medium mb-1">Height</label>
                        <Input type="number" id="sizeZ" class="w-full p-2 border rounded dark:border-gray-600" bind:value={sizeZ} placeholder="Height (cm)" />
                    </div>
                </div>
            </div>

            <!-- Replace weight dropdown with numeric input -->
            <div class="mb-4">
                <label for="weight" class="block text-sm font-medium mb-2">Weight (grams)</label>
                <Input type="number" id="weight" class="w-full p-2 border rounded dark:border-gray-600" bind:value={weight} placeholder="Weight in grams" />
            </div>
            
            <!-- Additional Options -->
            <div class="mb-4 grid grid-cols-1 lg:grid-cols-2 gap-4">
                <div class="flex items-center">
                    <input type="checkbox" bind:checked={handmade} id="handmade-checkbox" class="mr-2" />
                    <label for="handmade-checkbox" class="text-sm">Handmade</label>
                </div>
                <div class="flex items-center">
                    <input type="checkbox" bind:checked={oneOfAKind} id="oneofakind-checkbox" class="mr-2" />
                    <label for="oneofakind-checkbox" class="text-sm">One of a kind</label>
                </div>
            </div>

            <div>
                <label for="description" class="block text-sm font-medium mb-2">Description*</label>
                <Textarea id="description" class="w-full p-2 mb-4 border rounded dark:border-gray-600 h-auto" bind:value={description} placeholder="You can add any additional context about your object or how you came into possession of it."/>
                {#if errors.description}
                    <p class="text-red-500 text-sm mt-1">{errors.description}</p>
                {/if}
            </div>
            
            <!-- Tags -->
            <div class="mb-4">
                <label for="tags" class="block text-sm font-medium mb-2">Tags</label>
                <Query bind:tags={tags} bind:labels={labels} />
            </div>

            <!-- Anonymous Checkbox -->
            <div class="mb-6 flex items-center">
                <input type="checkbox" bind:checked={anonymous} id="anonymous-checkbox" class="mr-2" />
                <label for="anonymous-checkbox" class="text-sm">Post anonymously</label>
            </div>

            <!-- Image upload with required indicator -->
            <div class="mb-4">
                <label for="image-upload" class="block text-sm font-medium mb-2">Upload Image*</label>
                <input 
                    type="file" 
                    id="image-upload" 
                    accept="image/*" 
                    on:change={handleFileChange}
                    class="w-full p-2 border rounded dark:border-gray-600"
                />
                {#if errors.image}
                    <p class="text-red-500 text-sm mt-1">{errors.image}</p>
                {/if}
                <p class="text-xs text-gray-500 mt-1">Upload an image of your mystery object (required)</p>
            </div>

            <!-- Submit Button -->
            <div>
                <Button
                on:click={handlePost}
                variant="outline"
                size="icon"
                class="w-full p-4 bg-black dark:bg-white text-white dark:text-black hover:bg-rose-900 hover:dark:bg-rose-900 hover:text-white transition-colors rounded shadow">
                Post Your Stuff
            </Button>
            </div>
        </div>
    </Card.Root>
    </form>
</div>